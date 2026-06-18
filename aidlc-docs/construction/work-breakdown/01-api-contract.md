# API 계약 (REST API Contract)

> **목적**: 프론트엔드와 백엔드가 병렬로 개발할 수 있도록 API 인터페이스를 사전 확정한다.  
> **원칙**: 이 문서가 프론트↔백엔드의 단일 합의 기준이다. 변경 시 양측 협의 필수.  
> **공통**: Base URL `/api/v1`, 인증 헤더 `Authorization: Bearer <JWT>`, 모든 시각은 KST(ISO 8601 +09:00), Content-Type `application/json`.

---

## 0. 공통 응답 규약

### 성공 응답
```json
{ "data": { ... }, "timestamp": "2026-06-18T15:00:00+09:00" }
```

### 에러 응답
```json
{ "error": { "code": "INQUIRY_NOT_FOUND", "message": "문의를 찾을 수 없습니다." }, "timestamp": "..." }
```

### 페이지네이션 응답
```json
{ "data": [ ... ], "page": 0, "size": 20, "totalElements": 53, "totalPages": 3 }
```

---

## 1. 인증 API (담당: 백엔드 A)

### POST /api/v1/auth/login
- 인증: 불필요
- 요청: `{ "username": "string", "password": "string" }`
- 응답 200: `{ "data": { "token": "jwt...", "operator": { "id", "username", "role" }, "expiresAt": "..." } }`
- 에러: `INVALID_CREDENTIALS`(401), `ACCOUNT_LOCKED`(423)

### POST /api/v1/auth/logout
- 인증: 필요
- 응답 200: `{ "data": { "success": true } }`

---

## 2. 고객 문의 API (담당: 백엔드 A)

### POST /api/v1/inquiries
- 인증: 불필요 (고객용)
- 요청: `{ "customerInfo": { "userId": "string", "nickname": "string", "channel": "string" }, "customerType": "PAYMENT|ACCOUNT|ITEM_DELIVERY|ETC", "content": "string(최소 10자)" }`
- 응답 201: `{ "data": { "inquiryId": "uuid", "status": "RECEIVED", "createdAt": "..." } }`
- 에러: `VALIDATION_ERROR`(400)

### GET /api/v1/inquiries/{inquiryId}
- 인증: 필요
- 응답 200: InquiryDetail (아래 §6 DTO 참조)
- 에러: `INQUIRY_NOT_FOUND`(404)

---

## 3. 운영자 워크플로우 API (담당: 백엔드 C)

### POST /api/v1/operator/inquiries/pull
- 인증: 필요. AI 분석 완료된 미배정 문의 1건을 본인에게 배정(Pull).
- 요청: (없음, 토큰의 운영자 기준)
- 응답 200: InquiryDetail / 204: 배정 가능 문의 없음
- 동작: `PENDING_ASSIGNMENT` → `OPERATOR_REVIEWING`

### PATCH /api/v1/operator/inquiries/{inquiryId}/draft
- 인증: 필요. 답변 초안 수정.
- 요청: `{ "content": "string" }`
- 응답 200: `{ "data": { "draftId", "content", "status": "EDITED" } }`

### POST /api/v1/operator/inquiries/{inquiryId}/approve
- 인증: 필요. 답변 승인 → 발송 트리거.
- 요청: `{ "content": "string(선택, 수정후승인 시)" }`
- 응답 200: `{ "data": { "inquiryId", "status": "APPROVED" } }`
- 에러: `INVALID_STATE_TRANSITION`(409)

### POST /api/v1/operator/inquiries/{inquiryId}/reject
- 인증: 필요. 반려 → AI 재생성.
- 요청: `{ "reason": "string(필수)" }`
- 응답 200: `{ "data": { "inquiryId", "newDraft": { "draftId", "content", "regenerationCount" } } }`
- 에러: `REASON_REQUIRED`(400), `REGENERATION_LIMIT_EXCEEDED`(409)

### POST /api/v1/operator/inquiries/{inquiryId}/reanalyze
- 인증: 필요. 운영자 수동 재분석 요청.
- 요청: `{ "reason": "string(선택)" }`
- 응답 200: `{ "data": { "inquiryId", "status": "AI_ANALYZING" } }`

### GET /api/v1/operator/inquiries/{inquiryId}/history
- 인증: 필요. 처리 이력 타임라인.
- 응답 200: `{ "data": [ { "action", "operator", "reason", "timestamp" } ] }`

---

## 4. 대시보드 API (담당: 백엔드 C)

### GET /api/v1/dashboard/board
- 인증: 필요. 칸반 보드 데이터(상태별 그룹).
- 응답 200: `{ "data": { "RECEIVED": [card...], "AI_ANALYZING": [...], "PENDING_ASSIGNMENT": [...], "OPERATOR_REVIEWING": [...], "APPROVED": [...], "SENT": [...] } }`
- card: `{ "inquiryId", "customerType", "aiType", "urgency", "summary", "status", "assignedOperator", "createdAt" }`

### GET /api/v1/dashboard/inquiries
- 인증: 필요. 목록 조회 (필터/검색/페이징).
- 쿼리: `?status=&urgency=&type=&assignee=&keyword=&from=&to=&page=0&size=20`
- 응답 200: 페이지네이션 응답 (card 배열)

### GET /api/v1/dashboard/notifications
- 인증: 필요. 미배정/긴급 카운트.
- 응답 200: `{ "data": { "unassignedCount": 5, "urgentCount": 2 } }`

---

## 5. 내부 파이프라인 (담당: 백엔드 B — 외부 API 아님)

AI 파이프라인(분석/조회/진단/초안생성)은 문의 접수 후 **비동기 내부 처리**이며 외부 REST 엔드포인트가 아니다. 단, 상태 변화는 위 조회 API로 노출된다. 백엔드 B는 서비스 인터페이스(아래)로 A/C와 연동:
- `AIAnalysisService.analyze(inquiryId)` → 비동기 트리거
- 결과는 `AIAnalysis`, `Diagnosis`, `DraftResponse` 엔티티로 저장 (공유 스키마 참조)

---

## 6. 공유 DTO: InquiryDetail
```json
{
  "data": {
    "inquiry": { "inquiryId", "customerInfo", "customerType", "content", "status", "createdAt", "assignedOperator" },
    "analysis": { "aiType", "subCategory", "urgency", "summary", "keywords": [], "systemQueryResult": {}, "failureType": null },
    "diagnosis": { "cause", "suggestedDirection", "confidence" },
    "currentDraft": { "draftId", "content", "status", "regenerationCount" },
    "history": [ { "action", "operator", "reason", "timestamp" } ]
  }
}
```

> 미완료 단계의 필드는 `null`로 반환 (예: 분석 전이면 analysis=null).
