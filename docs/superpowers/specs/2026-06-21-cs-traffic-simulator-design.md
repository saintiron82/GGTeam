# CS 트래픽 시뮬레이터 (가상 등록기) — 설계 스펙

- 작성일: 2026-06-21
- 상태: 승인됨 (구현 계획 수립 단계)
- 범위: 백엔드(Spring) 내장 시뮬레이터 + agentcli LLM 클라이언트 + 프론트 제어판

## 1. 목표

`sim` 프로파일로 백엔드를 기동하면:

1. **가상 계정 100개 + 구매(결제)이력 + 아이템지급 ~500건**을 시드한다.
2. 그 데이터와 **정합되는 100건의 CS 문의**를 메모리에 준비한다.
3. 프론트 제어판에서 "시작"을 누르면 **20분에 걸쳐 한 건씩 실제 `POST /api/v1/inquiries`로 주입**한다.
4. 전체 AI 파이프라인(분류→시스템조회→진단→초안)·상태전이·칸반/대시보드가 실시간으로 채워진다.
5. 기본 LLM은 **agentcli(`claude -p`)**.

## 2. 확정된 설계 결정

| 결정 | 값 | 근거 |
|---|---|---|
| 구현 위치 | 백엔드 내장 (Spring), `@Profile("sim")` 게이트 | 백엔드 하나만 기동하면 끝, 별도 런타임 불필요 |
| 문의 주입 경로 | 실제 REST `POST /api/v1/inquiries` (RestClient 자기호출) | 전체 파이프라인·상태전이·대시보드 그대로 재현 |
| 기본 LLM | agentcli(`claude -p`) | Bedrock 자격증명 없이 진짜 LLM 품질 |
| 제어 방식 | 프론트 제어판(start/stop/status/reset). 부팅 시 시드만, 드립은 트리거 | 데모 시 원하는 순간에 트래픽 시작 |
| DB | `sim` 프로파일 = H2 인메모리 (`local`과 동일 방식) | 운영 DB·docker 불필요, 가장 간단 |
| 동시성 | `spring.task.execution.pool.core-size=3,max-size=4` | 동시 `claude` 프로세스 폭주 방지 (코드 변경 없음) |
| 기본 건수 | count=100 (제어판에서 축소 가능) | 요금 주의 — 리허설은 작은 count + mock 권장 |

## 3. 기존 코드 사실 (변경 없이 활용)

- 문의 접수: `POST /api/v1/inquiries` → `{customerInfo:{userId,...}, customerType, content}`, content 최소 10자, `RECEIVED` 저장 후 `AIAnalysisService.analyze` 비동기 트리거. (SecurityConfig permitAll)
- AI 분석: `userId`로 `PaymentQueryStrategy`가 `payment`/`item_delivery` 조회 → 진단·초안. **PAYMENT만 조회 전략 보유.**
- `InquiryType` = PAYMENT · ITEM_DELIVERY · ACCOUNT · ETC
- LLM 선택: `@ConditionalOnProperty(app.ai.llm-client = mock|bedrock)` → `agentcli` 값 추가로 끼움
- `LlmResponseParser`는 첫 `{`~마지막 `}`만 추출 → agentcli가 앞뒤 설명 붙여도 JSON 파싱 OK (초안은 평문 그대로)
- `@Async`는 기본 `applicationTaskExecutor` 사용 → `spring.task.execution.pool.*`로 제한
- `claude` CLI 확인: `/Users/saintiron/.local/bin/claude` v2.1.185
- 데모 테이블: `account(userId,status,lastLogin)`, `payment(userId,amount,status,errorLog,createdAt)`, `item_delivery(paymentId,userId,itemId,status,createdAt)`
- 대시보드 `/stats` 엔드포인트 존재(작업 중) → 제어판 실시간 지표로 활용

## 4. 컴포넌트 (백엔드, 모두 `sim` 게이트)

| 컴포넌트 | 책임 |
|---|---|
| `sim/SimProperties` | `app.sim.*` 바인딩(account-count, item-count, inquiry-count, duration-minutes, jitter, auto-start) |
| `sim/SimDataSeeder` (`@Profile("sim")`, CommandLineRunner) | 부팅 1회: Account 100 + Payment(구매이력) + ItemDelivery ~500 시드. 시나리오별 정합 |
| `sim/InquiryScenarioCatalog` | 시나리오 아카이브 → `List<PlannedInquiry>` 100건 결정적 생성 (별도 테이블 없음) |
| `sim/PlannedInquiry` (record) | `{userId, type, content, tag}` |
| `sim/SimulationService` | 드립 엔진: `ScheduledExecutorService`로 `window/count` 간격마다 1건 RestClient POST. 진행상태 보유. start/stop/status/reset |
| `sim/SimulationController` | `POST /api/v1/dev/simulation/{start,stop,reset}`, `GET .../status` |
| `sim/dto/SimulationStatus` (record) | `{running, total, sent, errors, startedAt, etaSeconds, ratePerMin, llmClient}` |
| `external/AgentCliClient` (`@ConditionalOnProperty(app.ai.llm-client=agentcli)`) | `ProcessBuilder`로 `claude -p`, system=`--append-system-prompt`, 프롬프트 stdin, stdout 캡처, 타임아웃 바운드. 타임아웃/실패→`LlmTimeoutException`/`LlmApiException` |

## 5. 데이터 & 시나리오 정합성

userId = `sim-user-0001` … `sim-user-0100`. ItemDelivery 총 ~500개가 100계정에 분산. 본문은 시나리오별 5~10개 템플릿을 변주.

| 태그 | 심는 데이터 | 본문 예 | 유형 | 비중 |
|---|---|---|---|---|
| PAID_NOT_DELIVERED | Payment SUCCESS + Delivery NOT_DELIVERED | "결제했는데 아이템이 안 들어왔어요" | PAYMENT | 25 |
| DUPLICATE_CHARGE | 동일금액 SUCCESS 2건 + Delivery 1건 | "두 번 청구됐어요, 환불해주세요" | PAYMENT | 15 |
| PAYMENT_FAILED | Payment FAILED(errorLog) | "실패 떴는데 돈이 빠졌어요" | PAYMENT | 12 |
| PARTIAL_DELIVERY | Delivery PARTIAL | "일부만 지급됐어요" | PAYMENT | 10 |
| REFUND_PENDING | Payment REFUNDED/환불대기 | "환불 신청했는데 아직이에요" | PAYMENT | 10 |
| POINT_NOT_CHARGED | 포인트 결제 SUCCESS + Delivery NOT_DELIVERED | "포인트 충전이 안 됐어요" | PAYMENT | 8 |
| WRONG_ITEM | Delivery itemId 불일치 | "다른 아이템이 왔어요" | PAYMENT | 5 |
| ACCOUNT_ISSUE | Account SUSPENDED/DORMANT/BANNED | "로그인이 안 돼요/정지됐어요" | ACCOUNT | 10 |
| ETC | — | 일반 문의 | ETC | 5 |

각 계정은 자기 시나리오에 맞는 구매이력을 갖는다(정합). 분포 합 = 100.

## 6. 드립(등록기) 동작

- 기본 100건/20분 → 12초 간격. `jitter` 옵션 시 ±수초 랜덤.
- start 파라미터: `count`, `durationMinutes`, `jitter`, (선택)`llmClient`.
- 각 tick = 1 HTTP POST `http://localhost:{server.port}/api/v1/inquiries`. 201이면 sent++, 실패는 errors++ 후 계속.
- 분석은 백엔드 비동기로 뒤따름 → 칸반 RECEIVED→AI_ANALYZING→PENDING_ASSIGNMENT 흐름 관찰.

## 7. agentcli(`claude -p`) 클라이언트

- 명령(설정): `app.ai.agentcli.command=claude`, 인자 `-p --append-system-prompt <system> --output-format text`(필요 시), 프롬프트 stdin.
- stdout 전체를 `LlmResponse.content`로. 비정상 종료/타임아웃 → 예외 매핑.
- 동시성: `application-sim.yml`에서 풀 3~4로 제한.
- ⚠️ 100건×3호출 ≈ 300회 `claude` 실행 = 실제 토큰/요금. 리허설은 count 작게 + `llm-client=mock` 권장.

## 8. 프론트 제어판

- 파일: `frontend/src/dev/SimulationControlPage.tsx`, 라우트 `/dev/sim` (`main.tsx` 추가).
- 입력: 건수, 소요(분), jitter, LLM(agentcli/mock). 버튼: 시작/정지/리셋.
- 폴링(2~3초): 진행바(sent/total), 경과/남은시간, 분당 유입, 에러 수 + 대시보드 `/stats` 분포 미니 위젯.
- `frontend/src/common/api.ts`에 sim 제어 API 추가, 실 백엔드 호출(MSW 우회).

## 9. 안전장치

- 시더·컨트롤러·AgentCliClient 모두 `sim` 프로파일/`@ConditionalOnProperty`로 격리 → 운영 빌드 비노출.
- `application-sim.yml`: `local`처럼 자급(H2 인메모리, 보안 비활성, X-Operator-Id) + `llm-client=agentcli` + 시드/드립/풀 설정. 운영 DB·자격증명 불필요.
- 기존 `local` 시더/파이프라인/대시보드 무변경.

## 10. 설정 (`application-sim.yml`)

```yaml
app:
  sim:
    account-count: 100
    item-count: 500
    inquiry-count: 100
    duration-minutes: 20
    jitter: false
    auto-start: false
  ai:
    llm-client: agentcli
    agentcli:
      command: claude
      timeout-seconds: 120
spring:
  task:
    execution:
      pool:
        core-size: 3
        max-size: 4
```

## 11. 신규/수정 파일

- 신규(백엔드): `sim/SimProperties.java`, `sim/SimDataSeeder.java`, `sim/InquiryScenarioCatalog.java`, `sim/PlannedInquiry.java`, `sim/SimulationService.java`, `sim/SimulationController.java`, `sim/dto/SimulationStatus.java`, `external/AgentCliClient.java`, `resources/application-sim.yml`
- 신규(프론트): `frontend/src/dev/SimulationControlPage.tsx`
- 수정(소폭): `frontend/src/common/api.ts`(+sim API), `frontend/src/main.tsx`(+라우트)
- 무변경: 기존 `local` 시더, AI 파이프라인, 대시보드

## 12. 테스트

- `InquiryScenarioCatalog`: 100건·태그분포·userId↔데이터 정합 단위테스트
- `SimDataSeeder`: 시드 카운트(100/≈500) 검증
- `AgentCliClient`: 명령을 `echo`/스텁으로 치환해 ProcessBuilder 경로 검증(실제 claude 미호출)
- `SimulationService`: 짧은 window로 tick 수·정지 동작 검증

## 13. 미해결/리스크

- agentcli 300회 호출 요금/지연 — 동시성 제한·count 축소·mock 토글로 완화.
- H2 인메모리는 재기동 시 초기화 — 데모 1회성으로 충분(의도된 동작).
