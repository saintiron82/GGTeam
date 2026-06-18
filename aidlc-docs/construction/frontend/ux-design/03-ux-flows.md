# 03. UX 플로우 & 인터랙션 명세

> 상태전이 기준: `02-shared-contracts.md §5`. 버튼 활성/비활성은 이 표를 단일 기준으로 한다.

## 1. 고객 문의 접수 플로우

```
폼 입력 → [제출]
  ├─ 검증 실패(유형 미선택/내용<10자) → 인라인 에러, 제출 막힘
  └─ 검증 통과 → POST /inquiries
       ├─ 201 → 완료 화면(문의 ID 표시)
       └─ 400 VALIDATION_ERROR → 에러 메시지
```

## 2. 운영자 로그인 플로우

```
[로그인] → POST /auth/login
  ├─ 200 → 토큰 저장 → /board 이동
  ├─ 401 INVALID_CREDENTIALS → "아이디 또는 비밀번호가 올바르지 않습니다"
  └─ 423 ACCOUNT_LOCKED → "계정이 잠겼습니다. 관리자에게 문의하세요"
토큰 만료(모든 API 401 UNAUTHORIZED) → /login 리다이렉트
```

## 3. 운영자 처리 플로우 (핵심)

```
칸반 보드(GET /dashboard/board)
  → 카드 클릭 → 상세(GET /inquiries/:id)
     → [담당하기] (PENDING_ASSIGNMENT일 때만)
         POST /operator/inquiries/pull → OPERATOR_REVIEWING
     → 답변 검토
         ├─ [수정] PATCH .../draft → 초안 갱신(EDITED)
         ├─ [승인]/[수정 후 승인] POST .../approve → APPROVED → (자동) SENT
         ├─ [반려 및 재생성] 사유 입력 → POST .../reject → 새 초안(regenerationCount+1)
         │     └─ 3회 초과 시 409 REGENERATION_LIMIT_EXCEEDED → 버튼 비활성
         └─ [재분석] POST .../reanalyze → AI_ANALYZING
```

## 4. 상태별 버튼 활성 규칙 (구현 필수)

| 현재 상태 | 담당하기 | 수정 | 승인 | 수정후승인 | 반려·재생성 | 재분석 |
|-----------|:--:|:--:|:--:|:--:|:--:|:--:|
| RECEIVED | ✕ | ✕ | ✕ | ✕ | ✕ | ✕ |
| AI_ANALYZING | ✕ | ✕ | ✕ | ✕ | ✕ | ✕ |
| PENDING_ASSIGNMENT | ✓ | ✕ | ✕ | ✕ | ✕ | ✕ |
| OPERATOR_REVIEWING | ✕ | ✓ | ✓ | ✓ | ✓¹ | ✓ |
| APPROVED | ✕ | ✕ | ✕ | ✕ | ✕ | ✕ |
| SENT | ✕ | ✕ | ✕ | ✕ | ✕ | ✕ |
| MANUAL_CLASSIFICATION_PENDING | ✓² | ✕ | ✕ | ✕ | ✕ | ✓ |

- ¹ regenerationCount ≥ 3 이면 비활성
- ² 수동배정 경로 (담당자 직접 배정)
- 그 외 상태는 모든 액션 버튼 비활성(읽기 전용)

## 5. 공통 상태 표시 규칙 (로딩/에러/빈 상태)

| 상황 | 표시 |
|------|------|
| 로딩 중 | 스켈레톤 또는 스피너 + "불러오는 중..." |
| 데이터 없음(빈 컬럼/목록) | "표시할 문의가 없습니다" 안내 |
| 분석 전 (analysis=null) | "AI 분석 대기 중" |
| 초안 없음 (currentDraft=null) | "답변 초안 생성 전" |
| API 에러(4xx/5xx) | 에러코드별 메시지(02 §3) + 재시도 버튼 |
| 동시성 충돌(409 ASSIGNMENT_CONFLICT) | "이미 다른 운영자가 담당 중입니다" → 보드 새로고침 |
| 권한 없음(403 FORBIDDEN) | "권한이 없습니다" |

## 6. 동시성 인터랙션 (US-15)

- 두 운영자가 동시에 "담당하기" 클릭 → 한 명만 성공
- 실패자: 409 ASSIGNMENT_CONFLICT → 토스트 + 보드 갱신으로 해당 카드 상태 업데이트
