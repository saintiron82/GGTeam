# Business Rules — Backend Unit

검증 규칙, 상태 전이 규칙, 비즈니스 정책을 정의합니다. 각 규칙에는 고유 ID(BR-xx)를 부여하여 추적 가능하게 합니다.

---

## 1. 입력 검증 규칙 (Validation)

| ID | 규칙 | 위반 시 처리 |
|----|------|-------------|
| BR-01 | 문의 본문(`content`)은 최소 10자 이상이어야 한다. | 접수 거부, 검증 오류 반환 |
| BR-02 | `customerType`(InquiryType)은 정의된 enum 값이어야 한다. | 접수 거부 |
| BR-03 | `customerInfo`는 고객 식별자(userId)를 포함해야 한다. | 접수 거부 |
| BR-04 | MVP에서 `PAYMENT` 외 유형은 접수는 가능하나 E2E 자동처리 대상이 아니다. | 분류 후 수동 처리 경로 |
| BR-05 | `Diagnosis.confidence`는 0.0 이상 1.0 이하여야 한다. | 저장 거부 |
| BR-06 | 운영자 `username`은 유일(UNIQUE)해야 한다. | 생성 거부 |

---

## 2. 상태 전이 규칙 (State Transition)

### 2.1 전이 테이블 (InquiryStatus)

| 현재 상태 | 허용되는 다음 상태 | 트리거 |
|-----------|-------------------|--------|
| `RECEIVED` | `AI_ANALYZING` | AI 분석 시작 |
| `AI_ANALYZING` | `PENDING_ASSIGNMENT`, `MANUAL_CLASSIFICATION_PENDING` | 분석 성공 / 분석 실패 |
| `PENDING_ASSIGNMENT` | `OPERATOR_REVIEWING`, `AI_ANALYZING` | Pull 배정 성공 / 운영자 재분석 요청 |
| `OPERATOR_REVIEWING` | `OPERATOR_REVIEWING`, `APPROVED`, `AI_ANALYZING` | 수정/반려(자기루프) / 승인 / 재분석 요청 |
| `APPROVED` | `SENT` | 발송 완료 |
| `MANUAL_CLASSIFICATION_PENDING` | `OPERATOR_REVIEWING`, `PENDING_ASSIGNMENT`, `AI_ANALYZING` | 운영자 수동 배정 / 재분류 후 배정 / 재분석 요청 |
| `SENT` | (없음 — 종료 상태) | — |

| ID | 규칙 |
|----|------|
| BR-07 | 위 전이 테이블에 정의되지 않은 상태 전이는 모두 금지한다(InquiryStateMachine이 강제). |
| BR-08 | `SENT`는 종료 상태이며 이후 어떤 전이도 불가하다. |
| BR-09 | 모든 상태 전이는 InquiryStateMachine을 통해서만 수행한다(직접 status 변경 금지). |
| BR-10 | `OPERATOR_REVIEWING`에서의 반려/수정은 상태를 변경하지 않고 자기루프로 처리한다. |

---

## 3. 배정 규칙 (Assignment)

| ID | 규칙 |
|----|------|
| BR-11 | 문의 배정은 Pull 방식이다(운영자가 가져감, 시스템이 강제 푸시하지 않음). |
| BR-12 | 하나의 문의는 동시에 최대 1명의 운영자에게만 배정된다(중복 배정 금지). |
| BR-13 | 배정 선택 우선순위: 긴급도(HIGH→NORMAL→LOW) → 접수 시각 오래된 순(FIFO). |
| BR-14 | 배정 성공 시 `assignedOperatorId` 설정과 `OPERATOR_REVIEWING` 전이는 원자적으로 처리한다. |
| BR-15 | 배정 시 ApprovalHistory(action=`ASSIGN`)를 반드시 기록한다. |

---

## 4. 승인·반려·재생성 규칙 (Approval / Rejection / Regeneration)

| ID | 규칙 |
|----|------|
| BR-16 | 반려(`REJECT`) 시 사유(`reason`)는 필수다. 사유 없는 반려는 거부한다. |
| BR-17 | 반려 시 해당 초안은 `REJECTED` 처리되고, 반려 사유를 컨텍스트로 새 초안을 재생성한다. |
| BR-18 | 재생성 시 `regenerationCount`를 1 증가시킨다. |
| BR-19 | 재생성 횟수는 최대 3회로 제한한다. 초과 시 자동 재생성을 중단하고 운영자 수동 작성으로 전환한다. |
| BR-20 | 수정(`EDIT`)은 `regenerationCount`를 증가시키지 않는다. |
| BR-21 | 승인은 활성(current) 초안이 존재할 때만 가능하다. |
| BR-22 | 모든 운영자 액션(승인/반려/수정/재생성/배정)은 ApprovalHistory에 불변(append-only)으로 기록한다. |

---

## 5. 발송 정책 (Dispatch)

| ID | 규칙 |
|----|------|
| BR-23 | AI 단독 발송 금지: 운영자 승인(`APPROVED`)을 거치지 않은 답변은 절대 발송하지 않는다. |
| BR-24 | 발송은 `APPROVED` 상태에서만 수행 가능하다. |
| BR-25 | 발송 완료 후 상태는 `SENT`로 전이한다. |
| BR-25a | 발송 실패 시 NotificationService가 재시도하며, 발송 시도 이력(시도 횟수, 결과, 시각)을 기록한다. 재시도 한도 초과 시 운영자에게 발송 실패 알림을 노출한다. |
| BR-25b | 발송 완료(`SENT`) 후 동일 이슈의 추가 응대가 필요한 경우, 재발송이 아닌 새로운 문의(Inquiry)로 처리한다. 기존 문의의 처리 이력은 참조 정보로 연결한다. |
---

## 6. AI 분석 실패 정책 (AI Failure Handling)

| ID | 규칙 |
|----|------|
| BR-26 | LLM/조회 타임아웃은 최대 3회 재시도하며 exponential backoff를 적용한다. |
| BR-26a | 타임아웃 재시도 시 타임아웃 값을 점증한다(예: 120s → 180s → 240s). |
| BR-27 | API 에러는 재시도 없이 즉시 실패 처리한다. |
| BR-28 | 재시도 소진(타임아웃) 또는 API 에러 시 `failureType`을 기록하고 `MANUAL_CLASSIFICATION_PENDING`로 전이한다. |
| BR-29 | 정상 분석 완료 시 `failureType`은 null이며 `analyzedAt`을 기록한다. |
| BR-30 | `MANUAL_CLASSIFICATION_PENDING` 문의는 운영자에게 수동 분류/배정 경로로 노출한다. |

## 6a. 운영자 재분석 정책 (Manual Reanalysis)

| ID | 규칙 |
|----|------|
| BR-30a | 운영자는 `MANUAL_CLASSIFICATION_PENDING`, `PENDING_ASSIGNMENT`, `OPERATOR_REVIEWING` 상태의 문의에 대해 재분석(`REANALYZE`)을 요청할 수 있다. |
| BR-30b | 재분석 시 기존 AIAnalysis/Diagnosis/DraftResponse는 이력으로 보존하고 새 분석으로 갱신한다. |
| BR-30c | 재분석 요청 시 상태를 `AI_ANALYZING`로 전이하고 ApprovalHistory(action=`REANALYZE`)를 기록한다. |

## 6b. 답변 품질 검증 정책 (Response Quality)

| ID | 규칙 |
|----|------|
| BR-30d | 답변 초안 생성 직후 자동 품질 검증을 수행한다(빈 응답, 최소 길이 미달, 금칙어, 구조 누락). |
| BR-30e | 품질 검증 불량 시 자동 1회 재생성하고, 그 후에도 불량이면 `MANUAL_CLASSIFICATION_PENDING`로 전이한다. |
| BR-30f | 운영자의 주관적 품질 불량 판단은 반려(BR-16, BR-17)로 처리한다. |

---

## 7. 인증·보안 정책 (Auth / Security — AuthService)

| ID | 규칙 |
|----|------|
| BR-31 | 로그인 연속 실패 5회 시 계정을 잠근다(`locked=true`). |
| BR-32 | 잠긴 계정(`locked=true`)은 로그인할 수 없으며 관리자 해제가 필요하다. |
| BR-33 | 로그인 성공 시 `failedLoginCount`를 0으로 초기화한다. |
| BR-34 | 인증 토큰(JWT)의 유효기간은 발급 시점부터 8시간이다. |
| BR-35 | 만료된 토큰으로의 요청은 거부하며 재인증을 요구한다. |
| BR-36 | 비밀번호는 평문 저장 금지, 단방향 해시(`passwordHash`)로만 저장한다. |
| BR-37 | 운영자 액션(배정/승인/반려 등)은 유효한 인증 토큰을 가진 운영자만 수행할 수 있다. |
| BR-38 | `ADMIN` 역할만 계정 잠금 해제 및 운영자 관리 작업을 수행할 수 있다. |

---

## 8. 데이터 무결성 정책 (Data Integrity)

| ID | 규칙 |
|----|------|
| BR-39 | 한 문의(Inquiry)당 AIAnalysis와 Diagnosis는 각각 최대 1건이다(1:1). |
| BR-40 | ApprovalHistory는 수정/삭제 불가능한 추가 전용 기록이다. |
| BR-41 | 모든 시각 데이터는 한국표준시로 저장한다. |
| BR-42 | `regenerationCount`는 음수가 될 수 없으며 단조 증가한다. |

---

## 9. 규칙 인덱스 (요약)

- 검증: BR-01 ~ BR-06
- 상태 전이: BR-07 ~ BR-10
- 배정: BR-11 ~ BR-15
- 승인/반려/재생성: BR-16 ~ BR-22
- 발송: BR-23 ~ BR-25
- AI 실패: BR-26 ~ BR-30
- 인증/보안: BR-31 ~ BR-38
- 데이터 무결성: BR-39 ~ BR-42
