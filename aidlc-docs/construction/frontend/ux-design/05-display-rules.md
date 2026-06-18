# 05. 표시 규칙 (라벨 · 색상 · 포맷)

> 기준: `02-shared-contracts.md §1 Enum`, `frontend/src/common/types.ts`.
> enum 값은 백엔드와 1:1. 한글 라벨·색상은 프론트에서 매핑한다.

## 1. 상태(InquiryStatus) 한글 라벨

| enum | 라벨 | 칸반 컬럼 |
|------|------|-----------|
| RECEIVED | 접수 | O |
| AI_ANALYZING | AI분석중 | O |
| PENDING_ASSIGNMENT | 담당자배정대기 | O |
| OPERATOR_REVIEWING | 운영자확인중 | O |
| APPROVED | 승인완료 | O |
| SENT | 발송완료 | O |
| MANUAL_CLASSIFICATION_PENDING | 수동분류대기 | 배지/별도 영역 |

## 2. 유형(InquiryType) 한글 라벨

| enum | 라벨 |
|------|------|
| PAYMENT | 결제 |
| ITEM_DELIVERY | 아이템지급 |
| ACCOUNT | 계정 |
| ETC | 기타 |

> 상세 화면에는 `customerType`(고객 선택)과 `aiType`(AI 분석)을 **모두** 표시. 처리 기준은 aiType 우선.

## 3. 긴급도(Urgency) 라벨 & 색상

| enum | 라벨 | 색상 | HEX |
|------|------|------|-----|
| HIGH | 긴급 | 빨강 | `#e74c3c` |
| NORMAL | 보통 | 주황 | `#f0ad4e` |
| LOW | 낮음 | 초록 | `#5cb85c` |

- 긴급(HIGH) 카드: 빨간 테두리 + 긴급 뱃지 강조
- 색상만으로 구분하지 않고 **항상 텍스트 라벨 병기** (접근성)

## 4. 날짜/시각 포맷 (KST)

- 모든 시각은 **KST(Asia/Seoul, +09:00)** 로 표시
- API는 ISO 8601(+09:00) 반환 → 화면 표시 포맷:
  - 목록/카드: `YYYY-MM-DD HH:mm` (예: 2026-06-18 15:00)
  - 타임라인: `MM-DD HH:mm` 또는 상대시간 보조("3분 전")
- 입력 시 타임존 변환 금지(서버가 KST 기준).

## 5. 이력 액션(ApprovalAction) 라벨

| enum | 라벨 |
|------|------|
| APPROVE | 승인 |
| REJECT | 반려 |
| EDIT | 수정 |
| REGENERATE | 재생성 |
| ASSIGN | 배정 |
| REANALYZE | 재분석 |

## 6. 답변 상태(DraftResponseStatus) 라벨

| enum | 라벨 |
|------|------|
| GENERATED | 생성됨 |
| EDITED | 수정됨 |
| REJECTED | 반려됨 |
| APPROVED | 승인됨 |

## 7. 실패 유형(FailureType) 표시

| enum | 표시 |
|------|------|
| TIMEOUT | "AI 분석 시간 초과" 배지 |
| API_ERROR | "AI 분석 오류" 배지 |
| null | (정상, 미표시) |

## 8. 에러 메시지 매핑 (사용자 노출 문구)

| code | 노출 문구 |
|------|-----------|
| VALIDATION_ERROR | 입력값을 확인해주세요 (필드별 상세) |
| REASON_REQUIRED | 반려 사유를 입력해주세요 |
| INVALID_CREDENTIALS | 아이디 또는 비밀번호가 올바르지 않습니다 |
| UNAUTHORIZED | 로그인이 필요합니다 (→ 로그인 이동) |
| FORBIDDEN | 권한이 없습니다 |
| INQUIRY_NOT_FOUND | 문의를 찾을 수 없습니다 |
| INVALID_STATE_TRANSITION | 현재 상태에서 처리할 수 없습니다 |
| REGENERATION_LIMIT_EXCEEDED | 재생성 가능 횟수를 초과했습니다 |
| ASSIGNMENT_CONFLICT | 이미 다른 운영자가 담당 중입니다 |
| ACCOUNT_LOCKED | 계정이 잠겼습니다. 관리자에게 문의하세요 |
| AI_SERVICE_ERROR | AI 분석 서비스에 일시적 문제가 있습니다 |
| INTERNAL_ERROR | 일시적인 오류가 발생했습니다 |
