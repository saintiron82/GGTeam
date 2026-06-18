# Business Logic Model — Backend Unit

AI 파이프라인 처리 로직과 운영자 워크플로우를 단계별로 기술합니다. 각 단계는 입력/처리/출력/상태전이로 구성되며, 기술 중립적으로 기술합니다.

> MVP 범위: 결제(`PAYMENT`) 유형만 end-to-end로 동작. 그 외 유형은 분류 후 `MANUAL_CLASSIFICATION_PENDING`로 처리 가능(확장 영역).

---

## 0. 전체 흐름 개요

```text
[접수] RECEIVED
   │ (InquiryService.create)
   ▼
[AI분석중] AI_ANALYZING ───(실패)──▶ MANUAL_CLASSIFICATION_PENDING
   │ (AIAnalysisService 파이프라인)
   ▼
[담당자배정대기] PENDING_ASSIGNMENT
   │ (ApprovalService.pullAssign — Pull 배정)
   ▼
[운영자확인중] OPERATOR_REVIEWING
   │   ▲  (반려 → AI 재생성 루프)
   │   └──────────────┐
   ▼                  │
[승인완료] APPROVED    │
   │                  │
   ▼                  │
[발송완료] SENT        │
                      │
   (REJECT) ──────────┘  draft 재생성 후 다시 OPERATOR_REVIEWING
```

---

## 1. AI 파이프라인 (자동 처리 구간)

### 단계 1-1. 문의 접수 (InquiryService)
- 입력: 고객 문의 페이로드(customerInfo, customerType, content)
- 처리:
  1. 입력 검증 (content 최소 10자 등 — business-rules 참조)
  2. Inquiry 생성, `status = RECEIVED`, `createdAt` 기록
  3. AI 분석 비동기 트리거
- 출력: 생성된 Inquiry(id)
- 상태전이: (없음) → `RECEIVED`

### 단계 1-2. AI 분석 시작 (AIAnalysisService)
- 입력: Inquiry.id
- 처리: 상태를 `AI_ANALYZING`로 전이 후 분류·조회·진단·초안 생성 순차 실행
- 출력: 파이프라인 진행
- 상태전이: `RECEIVED` → `AI_ANALYZING`

### 단계 1-3. 분류 및 요약 (AIAnalysisService → BedrockClient/LlmClient)
- 입력: Inquiry.content, customerInfo
- 처리:
  1. LlmClient 호출로 `aiType`, `subCategory`, `urgency`, `summary`, `keywords` 추출
  2. 재시도 정책 적용 (아래 §3)
- 출력: AIAnalysis 부분 채움
- 상태전이: `AI_ANALYZING` 유지 / 실패 시 §3 처리

### 단계 1-4. 시스템 데이터 조회 (SystemDataQueryService → QueryStrategy)
- 입력: `aiType`, customerInfo(userId 등)
- 처리:
  1. `aiType`에 맞는 QueryStrategy 선택 (MVP: PaymentQueryStrategy)
  2. 더미 시스템 테이블(Payment/ItemDelivery/Account) 조회
  3. 결과를 `systemQueryResult`(JSON)로 정규화
- 출력: AIAnalysis.systemQueryResult
- 상태전이: `AI_ANALYZING` 유지 / 조회 실패 시 §3 처리

### 단계 1-5. 진단 (DiagnosisService → LlmClient)
- 입력: AIAnalysis(요약/조회결과)
- 처리: 원인(`cause`), 처리방향(`suggestedDirection`), 신뢰도(`confidence`) 산출
- 출력: Diagnosis 생성
- 상태전이: `AI_ANALYZING` 유지

### 단계 1-6. 답변 초안 생성 (DraftResponseService → LlmClient)
- 입력: AIAnalysis + Diagnosis
- 처리: 답변 초안 생성, DraftResponse(`status=GENERATED`, `regenerationCount=0`) 저장
- 출력: DraftResponse
- 상태전이: `AI_ANALYZING` 유지

### 단계 1-7. 분석 완료 (AIAnalysisService → InquiryStateMachine)
- 입력: 완성된 AIAnalysis/Diagnosis/DraftResponse
- 처리: `analyzedAt` 기록, `failureType = null`
- 출력: 분석 결과 세트
- 상태전이: `AI_ANALYZING` → `PENDING_ASSIGNMENT`

---

## 2. 운영자 워크플로우 (사람 개입 구간)

### 단계 2-1. Pull 배정 (ApprovalService)
- 입력: 운영자 요청(다음 문의 가져오기)
- 처리: §4 동시성 처리에 따라 `PENDING_ASSIGNMENT` 문의 1건을 원자적으로 점유
- 출력: 배정된 Inquiry + 분석/진단/초안
- 상태전이: `PENDING_ASSIGNMENT` → `OPERATOR_REVIEWING`
- 이력: ApprovalHistory(action=`ASSIGN`)

### 단계 2-2. 검토 (운영자)
- 입력: 배정된 Inquiry, DraftResponse(current), Diagnosis, systemQueryResult
- 처리: 운영자가 초안/진단/조회결과를 검토하고 다음 중 하나 선택:
  - 승인 → §2-3
  - 수정 후 승인 → §2-4
  - 반려(재생성 요청) → §2-5
- 출력: 선택된 액션
- 상태전이: `OPERATOR_REVIEWING` 유지

### 단계 2-3. 승인 (ApprovalService)
- 입력: Inquiry.id, operatorId
- 처리: current DraftResponse `status=APPROVED`
- 출력: 승인 확정
- 상태전이: `OPERATOR_REVIEWING` → `APPROVED`
- 이력: ApprovalHistory(action=`APPROVE`)

### 단계 2-4. 수정 (DraftResponseService / ApprovalService)
- 입력: Inquiry.id, 수정된 content, operatorId
- 처리: DraftResponse.content 갱신, `status=EDITED` (재생성 카운트 미증가)
- 출력: 수정된 초안
- 상태전이: `OPERATOR_REVIEWING` 유지 (이후 승인 시 2-3로)
- 이력: ApprovalHistory(action=`EDIT`)

### 단계 2-5. 반려 → AI 재생성 루프 (ApprovalService → DraftResponseService)
- 입력: Inquiry.id, operatorId, reason(필수)
- 처리:
  1. current DraftResponse `status=REJECTED`
  2. ApprovalHistory(action=`REJECT`, reason 포함) 기록
  3. DraftResponseService가 반려 사유를 컨텍스트로 새 초안 생성, `regenerationCount += 1`
  4. ApprovalHistory(action=`REGENERATE`) 기록
  5. 재생성 한도(BR 참조) 초과 시 루프 중단 처리
- 출력: 새 DraftResponse(`status=GENERATED`)
- 상태전이: `OPERATOR_REVIEWING` 유지 (재생성된 초안으로 재검토)
- 이력: ApprovalHistory(action=`REJECT`) + (action=`REGENERATE`)

### 단계 2-6. 발송 (ApprovalService → NotificationService)
- 입력: APPROVED 상태 Inquiry
- 처리: 승인된 답변을 고객에게 전송. AI 단독 발송 금지(승인 선행 필수 — BR 참조)
- 출력: 발송 결과
- 상태전이: `APPROVED` → `SENT` (종료)

---

## 3. 재시도 로직 (AI/조회 호출)

LlmClient(BedrockClient) 및 SystemDataQueryService의 외부 호출에 적용.

| 오류 구분 | 정책 | 재시도 횟수 | 백오프 | 타임아웃 | 결과 |
|-----------|------|-------------|--------|----------|------|
| 타임아웃(TIMEOUT) | 재시도 | 최대 3회 | Exponential backoff | 점증(120s→180s→240s) | 3회 소진 시 `failureType=TIMEOUT` 확정 |
| API 에러(API_ERROR) | 즉시 실패 | 0회 | 없음 | — | `failureType=API_ERROR` 즉시 확정 |

처리 흐름:
```text
호출 시도
 ├─ 성공 → 다음 단계
 ├─ 타임아웃 → backoff 대기(예: 1s, 2s, 4s) + 타임아웃 값 점증(120→180→240s) 후 재시도 (최대 3회)
 │           └─ 3회 모두 실패 → failureType=TIMEOUT, analyzedAt=현재시각
 │                              → AI_ANALYZING → MANUAL_CLASSIFICATION_PENDING
 └─ API 에러 → 재시도 없이 failureType=API_ERROR
              → AI_ANALYZING → MANUAL_CLASSIFICATION_PENDING
```
- 실패 확정 시: AIAnalysis에 `failureType` 기록, 상태를 `MANUAL_CLASSIFICATION_PENDING`로 전이.
- 수동분류대기 문의는 운영자가 직접 분류/처리 (배정 풀에 별도 노출).

### 3.1 운영자 수동 재분석/재시도 (REANALYZE)
자동 재시도가 소진되었거나 운영자가 분석 결과가 불충분하다고 판단할 때, 직접 AI 파이프라인을 재실행하도록 요청할 수 있다.

- 대상 상태: `MANUAL_CLASSIFICATION_PENDING`, `PENDING_ASSIGNMENT`, `OPERATOR_REVIEWING`
- 처리:
  1. 운영자가 "재분석 요청" 실행
  2. 기존 AIAnalysis/Diagnosis/DraftResponse는 보존(이력 유지), 새 분석으로 갱신
  3. 상태를 `AI_ANALYZING`로 되돌려 파이프라인 §1-3~1-7 재실행
  4. ApprovalHistory(action=`REANALYZE`, reason 선택) 기록
- 상태전이: `MANUAL_CLASSIFICATION_PENDING`/`PENDING_ASSIGNMENT`/`OPERATOR_REVIEWING` → `AI_ANALYZING`

---

## 4. Pull 배정 동시성 처리

여러 운영자가 동시에 "다음 문의 가져오기"를 요청할 때 동일 문의 중복 배정을 방지한다.

원칙:
1. `PENDING_ASSIGNMENT` 문의 중 1건을 원자적(atomic) 점유로 선택.
2. 점유 성공 시에만 `assignedOperatorId` 설정 및 `OPERATOR_REVIEWING` 전이.
3. 동시 요청 중 경쟁에서 패배한 요청은 다음 후보 문의로 재시도.

논리적 동작 (구현 중립):
```text
원자적 갱신 조건:
  WHERE status = PENDING_ASSIGNMENT AND assignedOperatorId IS NULL
  SET   status = OPERATOR_REVIEWING, assignedOperatorId = :operatorId
영향 행 수 == 1 → 배정 성공 (해당 문의 반환)
영향 행 수 == 0 → 이미 타 운영자 점유 → 다음 후보 재시도
가용 문의 없음   → "배정 가능한 문의 없음" 응답
```
- 선택 우선순위: 긴급도(HIGH > NORMAL > LOW) → 접수 시각(오래된 순, FIFO).
- 배정 성공 시 ApprovalHistory(action=`ASSIGN`) 기록.

---

## 5. 핵심 시나리오: 결제 문의 End-to-End (MVP)

```text
1. 고객: "결제했는데 아이템이 안 들어왔어요" 접수
   → RECEIVED
2. AIAnalysisService: aiType=PAYMENT, subCategory=아이템미지급,
   urgency=NORMAL, summary/keywords 추출 → AI_ANALYZING
3. SystemDataQueryService(PaymentQueryStrategy):
   Payment(status=SUCCESS) + ItemDelivery(status=NOT_DELIVERED) 조회
   → systemQueryResult 채움
4. DiagnosisService: cause="결제 성공 but 지급 누락",
   suggestedDirection="아이템 재지급 안내", confidence=0.9
5. DraftResponseService: 답변 초안 생성 → PENDING_ASSIGNMENT
6. 운영자: Pull 배정 → OPERATOR_REVIEWING
7. 운영자: 초안 검토 후 (필요 시 수정) 승인 → APPROVED
8. NotificationService: 고객에게 답변 발송 → SENT
```

예외 분기:
- 2~5 단계 중 타임아웃 3회/ API 에러 → `MANUAL_CLASSIFICATION_PENDING`
- 6~7 단계에서 운영자 반려 → §2-5 재생성 루프

### 5.1 답변 품질 불량 대응 (AI 응답은 정상이나 내용 불량)
API 호출은 성공했으나 생성된 답변/분석 내용이 불량한 경우를 구분하여 처리한다.

- 자동 품질 검증 항목 (생성 직후 §1-6에서 수행):
  - 빈 응답 또는 공백만 존재
  - 최소 길이 미달 (예: 20자 미만)
  - 금칙어/부적절 표현 포함
  - 필수 구조 누락 (진단 결과와 무관한 응답)
- 불량 판정 시 처리:
  1. 자동 1회 재생성 시도 (`regenerationCount += 1`)
  2. 재생성 후에도 불량이면 `MANUAL_CLASSIFICATION_PENDING`로 전이하여 운영자 수동 작성/재분석(§3.1)으로 위임
- 운영자가 주관적으로 판단하는 품질 불량은 기존 반려(§2-5, BR-17)로 처리