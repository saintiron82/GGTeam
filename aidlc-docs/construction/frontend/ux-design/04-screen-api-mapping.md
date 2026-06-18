# 04. 화면-API 매핑표

> 기준: `work-breakdown/01-api-contract.md`. 각 화면이 호출하는 API와 트리거를 정의한다.
> 프론트 개발자는 MSW로 이 계약을 모킹하여 병렬 개발한다.

## 화면별 API 호출

| 화면 | 트리거 | Method & Endpoint | 인증 | 응답/에러 |
|------|--------|-------------------|------|-----------|
| S1 문의 폼 | 제출 | `POST /api/v1/inquiries` | 불필요 | 201 inquiryId / 400 VALIDATION_ERROR |
| S3 로그인 | 로그인 | `POST /api/v1/auth/login` | 불필요 | 200 token / 401 INVALID_CREDENTIALS / 423 ACCOUNT_LOCKED |
| (전역) | 로그아웃 | `POST /api/v1/auth/logout` | 필요 | 200 |
| S4 칸반 보드 | 진입/새로고침 | `GET /api/v1/dashboard/board` | 필요 | 상태별 카드 그룹 |
| S4 알림 배지 | 진입/폴링 | `GET /api/v1/dashboard/notifications` | 필요 | unassignedCount, urgentCount |
| S4 필터/검색 | 필터 변경 | `GET /api/v1/dashboard/inquiries?status=&urgency=&type=&assignee=&keyword=&from=&to=&page=&size=` | 필요 | 페이지네이션 카드 |
| S7 리스트 뷰 | 진입/페이징 | `GET /api/v1/dashboard/inquiries?...` | 필요 | 페이지네이션 카드 |
| S5 상세 | 진입 | `GET /api/v1/inquiries/{inquiryId}` | 필요 | InquiryDetail / 404 INQUIRY_NOT_FOUND |
| S5 이력 | 진입/탭 | `GET /api/v1/operator/inquiries/{inquiryId}/history` | 필요 | 이력 배열 |
| S5 담당하기 | [담당하기] | `POST /api/v1/operator/inquiries/pull` | 필요 | InquiryDetail / 204 없음 / 409 ASSIGNMENT_CONFLICT |
| S6 초안 수정 | [수정 저장] | `PATCH /api/v1/operator/inquiries/{inquiryId}/draft` | 필요 | draft(EDITED) |
| S6 승인 | [승인]/[수정후승인] | `POST /api/v1/operator/inquiries/{inquiryId}/approve` | 필요 | APPROVED / 409 INVALID_STATE_TRANSITION |
| S6 반려 | [반려 및 재생성] | `POST /api/v1/operator/inquiries/{inquiryId}/reject` | 필요 | newDraft / 400 REASON_REQUIRED / 409 REGENERATION_LIMIT_EXCEEDED |
| S6 재분석 | [재분석] | `POST /api/v1/operator/inquiries/{inquiryId}/reanalyze` | 필요 | AI_ANALYZING |

## InquiryDetail 필드 → 화면 영역 매핑 (S5)

| DTO 필드 | 화면 영역 | null일 때 |
|----------|-----------|-----------|
| inquiry | 원본 문의 영역 | (항상 존재) |
| analysis | AI 분석 결과 영역 | "AI 분석 대기 중" |
| analysis.systemQueryResult | 시스템 조회 결과 영역 | "조회 결과 없음" |
| diagnosis | AI 진단 영역 | "진단 대기 중" |
| currentDraft | 답변 편집기(S6) | "답변 초안 생성 전" |
| history | 타임라인 | "이력 없음" |

## 폴링/실시간 고려

- 알림 배지·보드는 MVP에서 **수동 새로고침 + 주기적 폴링(예: 30초)** 으로 처리.
- WebSocket/SSE는 Phase 2 검토 (계약에 미포함).
