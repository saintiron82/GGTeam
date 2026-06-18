# 서비스 정의 및 오케스트레이션 (Services)

> 서비스 계층의 책임 분배와 AI 파이프라인 오케스트레이션 패턴을 정의한다.

---

## 1. 서비스 분류

| 서비스 | 분류 | 핵심 책임 |
|---|---|---|
| InquiryService | 오케스트레이터(진입) | 문의 생성/조회, AI 파이프라인 트리거 |
| AIAnalysisService | 파이프라인 단계 1 | AI 자동 분류(분석) |
| SystemDataQueryService | 파이프라인 단계 2 | 유형별 시스템 데이터 조회(Strategy) |
| DiagnosisService | 파이프라인 단계 3 | AI 원인 진단 |
| DraftResponseService | 파이프라인 단계 4 | 답변 초안 자동 생성/재생성 |
| ApprovalService | 워크플로우 | 배정(Pull)/승인/반려, 이력 |
| NotificationService | 워크플로우 | 답변 발송, 알림 집계 |
| InquiryStateMachine | 횡단(Cross-cutting) | 상태 전이 규칙·기록 |
| AuthService | 횡단 | 인증/인가(JWT) |
| BedrockClient | 외부 연동 | LLM 호출 추상화 |

---

## 2. AI 파이프라인 오케스트레이션

### 2.1 오케스트레이션 패턴
- **패턴**: Pipeline (순차 단계 체인) + Orchestrator 주도(중앙 조정).
- **주관 서비스**: `InquiryService`가 파이프라인을 트리거하고, 각 단계 서비스를 순서대로 호출하는 **Orchestration(중앙 집중형)** 방식을 채택한다. (이벤트 기반 Choreography 대비 흐름 추적/디버깅이 쉬워 소규모 모놀리식에 적합)
- **실행 방식**: 문의 접수 응답은 즉시 반환(접수 완료), 파이프라인은 **비동기**(`@Async` 또는 작업 큐)로 백그라운드 실행. NFR-01(600초 이내) 충족 범위에서 진행.
- **트랜잭션**: 각 단계는 독립 트랜잭션으로 결과를 저장(부분 진행 보존). 단계 실패 시 이미 저장된 결과는 유지하고 상태만 실패 분기로 전이.

### 2.2 파이프라인 단계 흐름

```
[접수 완료]
    │  InquiryService.triggerAnalysisPipeline()
    ▼
(1) 분석   AIAnalysisService.analyze()
    │   - 상태: 접수 → AI분석중
    │   - 출력: ai_type, sub_category, urgency, summary, keywords
    │   - 실패: 타임아웃(재시도 3회) / API에러(즉시) → 수동분류대기 [파이프라인 중단]
    ▼
(2) 조회   SystemDataQueryService.query(ai_type)
    │   - Strategy 선택: PaymentQueryStrategy (MVP)
    │   - 출력: SystemQueryResult(결제이력/성공실패/오류로그)
    ▼
(3) 진단   DiagnosisService.diagnose(queryResult)
    │   - 출력: 원인, 처리방향, 신뢰도
    │   - 제약: 자동 발송 없음 (운영자 검토 전제)
    ▼
(4) 답변   DraftResponseService.generate(diagnosis)
    │   - 출력: 답변 초안(고객 친화 어조), DB 저장
    │   - 상태: AI분석중 → 담당자배정대기
    ▼
[담당자배정대기]  ← 운영자 Pull 대기
```

### 2.3 단계별 상태 전이 매핑

| 단계 | 시작 상태 | 종료 상태 | 실패 시 전이 |
|---|---|---|---|
| (1) 분석 | 접수 → AI분석중 | (다음 단계 계속) | 수동분류대기 |
| (2) 조회 | AI분석중 | (계속) | 수동분류대기(또는 진단 스킵 후 운영자 안내) |
| (3) 진단 | AI분석중 | (계속) | 담당자배정대기(초안 없이 운영자 처리) |
| (4) 답변 | AI분석중 | 담당자배정대기 | 담당자배정대기(초안 생성 실패 표기) |

> 단계 (2)~(4)의 LLM 호출 실패도 단계(1)과 동일한 타임아웃/API에러 구분 정책을 따른다.

---

## 3. 운영자 워크플로우 오케스트레이션

```
[담당자배정대기]
    │  ApprovalService.claim()  (Pull, 낙관적 락 동시성)
    ▼
[운영자확인중]
    │
    ├── 수정    DraftResponseService.update()        → (운영자확인중 유지, 이력 기록)
    │
    ├── 반려    ApprovalService.reject(reason)
    │              └─> DraftResponseService.regenerate(reason)  // 사유 AI 반영
    │              → (운영자확인중 유지, 재생성 횟수+1, 새 초안)
    │
    └── 승인    ApprovalService.approve()
                   │  상태: 운영자확인중 → 승인완료
                   └─> NotificationService.sendApprovedResponse()
                          → 상태: 승인완료 → 발송완료 (실패 시 재시도)
```

- **반려 재생성 루프**: 반려는 발송이 아닌 재생성으로 이어지며 운영자가 다시 검토한다(Human-in-the-loop 보장).
- **모든 이벤트**(배정/수정/승인/반려/재생성/상태전이)는 `ApprovalService` + `InquiryStateMachine`을 통해 타임라인 이력으로 기록된다(US-18, US-21).

---

## 4. 오케스트레이션 책임 경계

- **InquiryService**: 파이프라인 단계의 호출 순서와 흐름 제어(오케스트레이터). 단계 간 데이터 전달 조정.
- **각 단계 서비스**: 자기 단계의 비즈니스 로직만 책임(단일 책임). 다음 단계를 직접 호출하지 않음.
- **InquiryStateMachine**: 모든 상태 전이의 단일 통제점. 서비스는 직접 status를 쓰지 않고 StateMachine을 경유.
- **BedrockClient**: LLM 호출의 단일 통로. 분석/진단/답변/재생성 모두 이 클라이언트를 통해 모델 호출 → 모델 교체 시 단일 지점 변경(NFR-03).

---

## 5. 실패·복원 정책 요약 (MVP)

| 실패 유형 | 정책 |
|---|---|
| LLM 타임아웃(120초) | exponential backoff 최대 3회 재시도 |
| LLM API 에러(4xx/5xx) | 재시도 없이 즉시 실패, 에러유형 로깅 |
| 분석 최종 실패 | `수동분류대기` 전이, 운영자 수동 처리 |
| 발송 실패 | 재시도 메커니즘, 상태는 승인완료 유지 후 재발송 |
| 동시 배정 충돌 | 낙관적 락으로 중복 배정 방지(선점자 우선) |

> 고가용성/복원력 고도화(Resiliency Baseline)는 비활성(Phase 2 이후). MVP는 위 최소 정책으로 문의 누락 방지에 집중한다.

---

## 6. 비동기 처리 메모
- MVP는 Spring `@Async` + 단일 스레드풀로 시작(동시 5명, 동시 문의 처리량 낮음).
- 파이프라인 진행 상태는 `InquiryStatus`로 표현되어 프론트가 폴링/새로고침으로 진행을 확인.
- 향후 처리량 증가 시 메시지 큐(예: SQS/Rabbit) 도입 가능하도록 트리거 지점을 `triggerAnalysisPipeline()` 단일 메서드로 캡슐화.
