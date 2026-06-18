# 공유 계약: 데이터 스키마 · Enum · 에러코드 · 인증 규약

> **목적**: 백엔드 3명과 프론트 2명이 공유하는 데이터 정의의 단일 기준.  
> 상세 엔티티는 `aidlc-docs/construction/backend/functional-design/domain-entities.md` 참조. 본 문서는 팀 공유 요약본.

---

## 1. Enum (프론트/백엔드 공유)

> 프론트는 이 값들을 그대로 사용. 표시용 한글 라벨은 프론트에서 매핑.

### InquiryStatus (문의 상태)
`RECEIVED` | `AI_ANALYZING` | `PENDING_ASSIGNMENT` | `OPERATOR_REVIEWING` | `APPROVED` | `SENT` | `MANUAL_CLASSIFICATION_PENDING`

라벨: 접수 / AI분석중 / 담당자배정대기 / 운영자확인중 / 승인완료 / 발송완료 / 수동분류대기

### InquiryType (문의 유형)
`PAYMENT`(결제, MVP) | `ITEM_DELIVERY`(아이템지급) | `ACCOUNT`(계정) | `ETC`(기타)

### Urgency (긴급도)
`HIGH`(긴급) | `NORMAL`(보통) | `LOW`(낮음)

### ApprovalAction (이력 액션)
`APPROVE` | `REJECT` | `EDIT` | `REGENERATE` | `ASSIGN` | `REANALYZE`

### DraftResponseStatus
`GENERATED` | `EDITED` | `REJECTED` | `APPROVED`

### FailureType
`TIMEOUT` | `API_ERROR` | `null`(정상)

### OperatorRole
`OPERATOR` | `ADMIN`

---

## 2. 데이터 스키마 요약 (DB 테이블)

| 엔티티 | 담당 | 핵심 필드 |
|--------|------|-----------|
| Inquiry | 백엔드 A | id(UUID), customerInfo(JSON), customerType, content, status, createdAt, assignedOperatorId |
| AIAnalysis | 백엔드 B | id, inquiryId(UNIQUE), aiType, subCategory, urgency, summary, keywords(JSON), systemQueryResult(JSON), analyzedAt, failureType |
| Diagnosis | 백엔드 B | id, inquiryId(UNIQUE), cause, suggestedDirection, confidence(0~1) |
| DraftResponse | 백엔드 B | id, inquiryId, content, status, regenerationCount, createdAt |
| ApprovalHistory | 백엔드 C | id, inquiryId, action, operatorId, reason, timestamp (append-only) |
| Operator | 백엔드 A | id, username(UNIQUE), passwordHash, role, failedLoginCount, locked |
| Payment(더미) | 백엔드 B | id, userId, amount, status, errorLog, createdAt |
| ItemDelivery(더미) | 백엔드 B | id, paymentId, userId, itemId, status, createdAt |
| Account(더미) | 백엔드 A | id, userId(UNIQUE), status, lastLogin |

> 모든 Timestamp: KST(Asia/Seoul) 저장·표시.

---

## 3. 에러코드 (공유)

| code | HTTP | 의미 |
|------|------|------|
| `VALIDATION_ERROR` | 400 | 입력 검증 실패 (content 10자 미만 등) |
| `REASON_REQUIRED` | 400 | 반려 사유 누락 |
| `INVALID_CREDENTIALS` | 401 | 로그인 실패 |
| `UNAUTHORIZED` | 401 | 토큰 없음/만료 |
| `FORBIDDEN` | 403 | 권한 없음 (ADMIN 전용 등) |
| `INQUIRY_NOT_FOUND` | 404 | 문의 없음 |
| `INVALID_STATE_TRANSITION` | 409 | 허용되지 않은 상태 전이 |
| `REGENERATION_LIMIT_EXCEEDED` | 409 | 재생성 3회 초과 |
| `ASSIGNMENT_CONFLICT` | 409 | 이미 배정된 문의 (동시성) |
| `ACCOUNT_LOCKED` | 423 | 계정 잠금 (5회 실패) |
| `AI_SERVICE_ERROR` | 502 | Bedrock 호출 실패 |
| `INTERNAL_ERROR` | 500 | 서버 내부 오류 |

---

## 4. 인증 규약 (JWT)

- **방식**: Bearer Token. 헤더 `Authorization: Bearer <jwt>`
- **발급**: 로그인 성공 시
- **유효기간**: 8시간
- **Claims**: `sub`(operatorId), `username`, `role`, `iat`, `exp`
- **서명**: HS256, 서버 시크릿(`JWT_SECRET` 환경변수)
- **만료/무효 토큰**: 401 `UNAUTHORIZED` → 프론트는 로그인 페이지로 리다이렉트
- **비인증 허용 엔드포인트**: `POST /api/v1/inquiries`(고객 접수), `POST /api/v1/auth/login`
- **그 외 모든 엔드포인트**: 인증 필수

---

## 5. 상태 전이 규약 (프론트 UI 반영용)

| 현재 상태 | 가능한 액션(운영자) | 다음 상태 |
|-----------|---------------------|-----------|
| PENDING_ASSIGNMENT | Pull 배정 | OPERATOR_REVIEWING |
| OPERATOR_REVIEWING | 승인 | APPROVED |
| OPERATOR_REVIEWING | 수정 | OPERATOR_REVIEWING |
| OPERATOR_REVIEWING | 반려(사유) | OPERATOR_REVIEWING (재생성) |
| OPERATOR_REVIEWING | 재분석 | AI_ANALYZING |
| APPROVED | (자동 발송) | SENT |
| MANUAL_CLASSIFICATION_PENDING | 재분석/수동배정 | AI_ANALYZING / OPERATOR_REVIEWING |

> 프론트는 현재 상태에 따라 버튼 활성/비활성을 이 표 기준으로 제어한다.
