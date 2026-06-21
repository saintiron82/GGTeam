# CS 트래픽 시뮬레이터 (가상 등록기) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** `sim` 프로파일로 백엔드를 띄우면 가상 계정 100 + 구매이력 + 아이템지급 ~500을 시드하고, 정합되는 CS 문의 100건을 20분에 걸쳐 실제 REST로 드립 주입하며, agentcli(`claude -p`)로 AI 분석을 돌리고, 프론트 제어판에서 시작/정지/진행을 제어한다.

**Architecture:** 모든 백엔드 시뮬레이터 컴포넌트는 `@Profile("sim")`/`@ConditionalOnProperty`로 격리되어 운영 빌드에 노출되지 않는다. 시나리오 가중 분배(ScenarioAssigner)를 단일 진실원으로 시더(데이터)와 카탈로그(문의)가 공유한다. 드립은 `ScheduledExecutorService`가 `RestClient`로 자기 자신의 `POST /api/v1/inquiries`를 호출한다. 제어판은 절대 URL로 실 백엔드를 호출(MSW 우회)하고 sim 프로파일 CORS로 허용한다.

**Tech Stack:** Spring Boot 3.3.2 / JDK 17 / Gradle Wrapper · JUnit5 + Mockito + AssertJ · React 18 + Vite + Vitest + Testing Library · H2 인메모리(sim) · agentcli=`claude` CLI.

## Global Constraints

- 모든 시각: KST(Asia/Seoul). 엔티티 `createdAt`은 `@CreationTimestamp` 자동 — 수동 설정 금지.
- 문의 본문(content): 최소 10자 (BR-01). 모든 시나리오 템플릿은 10자 이상.
- 시뮬레이터 전 컴포넌트는 `@Profile("sim")` 또는 `@ConditionalOnProperty(app.ai.llm-client=agentcli)`로 게이트 — 운영 비노출.
- userId 규약: `sim-user-%04d` (`sim-user-0001` … ).
- agentcli 동시 실행은 `spring.task.execution.pool.core-size=3, max-size=4`로 제한 (코드 변경 없이 yml).
- 금지 용어: "고아(orphan)" 어떤 맥락에서도 사용 금지 — "참조 없는/매칭되지 않는/잔여"로 표현.
- 패키지: 신규 백엔드 코드는 `com.ggteam.cs.sim`, agentcli는 `com.ggteam.cs.external`.
- 명령 실행 위치: 백엔드 명령은 `backend/` 디렉터리에서, 프론트 명령은 `frontend/` 디렉터리에서 실행.

---

### Task 1: AgentCliClient (agentcli LLM 클라이언트)

**Files:**
- Create: `backend/src/main/java/com/ggteam/cs/external/AgentCliClient.java`
- Test: `backend/src/test/java/com/ggteam/cs/external/AgentCliClientTest.java`

**Interfaces:**
- Consumes: `LlmClient` (기존 `external/LlmClient.java`: `LlmResponse complete(LlmRequest)`, `LlmRequest{system,prompt,maxTokens}`, `LlmResponse{content}`), `LlmTimeoutException`, `LlmApiException` (기존).
- Produces: `AgentCliClient(String command, String systemFlag, int timeoutSeconds)` — `@ConditionalOnProperty(name="app.ai.llm-client", havingValue="agentcli")`.

- [ ] **Step 1: 실패 테스트 작성**

```java
package com.ggteam.cs.external;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ggteam.cs.external.LlmClient.LlmRequest;
import org.junit.jupiter.api.Test;

class AgentCliClientTest {

    @Test
    void stdin을_받아_stdout으로_반환한다() {
        // command를 `cat`으로 치환 → stdin(prompt)을 그대로 stdout으로 echo
        AgentCliClient client = new AgentCliClient("cat", "--append-system-prompt", 5);

        LlmClient.LlmResponse res = client.complete(LlmRequest.of("[CLASSIFY] 결제 문의입니다"));

        assertThat(res.content()).contains("[CLASSIFY] 결제 문의입니다");
    }

    @Test
    void 비정상_종료시_LlmApiException() {
        // `false`는 stdin 무시, exit 1
        AgentCliClient client = new AgentCliClient("false", "--append-system-prompt", 5);

        assertThatThrownBy(() -> client.complete(LlmRequest.of("hi")))
                .isInstanceOf(LlmApiException.class);
    }

    @Test
    void 타임아웃시_LlmTimeoutException() {
        AgentCliClient client = new AgentCliClient("sleep 5", "--append-system-prompt", 1);

        assertThatThrownBy(() -> client.complete(LlmRequest.of("hi")))
                .isInstanceOf(LlmTimeoutException.class);
    }
}
```

- [ ] **Step 2: 테스트 실패 확인**

Run: `./gradlew test --tests "com.ggteam.cs.external.AgentCliClientTest"`
Expected: 컴파일 실패 (`AgentCliClient` 없음).

- [ ] **Step 3: 구현 작성**

```java
package com.ggteam.cs.external;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 로컬 에이전트 CLI(`claude -p`) 기반 LlmClient (시뮬레이션/데모용).
 * Bedrock 자격증명 없이 실제 LLM 품질을 얻기 위해 외부 CLI 프로세스를 호출한다.
 *
 * <p>프롬프트는 stdin으로, 시스템 지침은 systemFlag 인자로 전달하고 stdout 전체를 응답으로 사용한다.
 * 활성화: app.ai.llm-client=agentcli
 */
@Component
@ConditionalOnProperty(name = "app.ai.llm-client", havingValue = "agentcli")
public class AgentCliClient implements LlmClient {

    private static final Logger log = LoggerFactory.getLogger(AgentCliClient.class);

    private final List<String> baseCommand;
    private final String systemFlag;
    private final int timeoutSeconds;

    public AgentCliClient(
            @Value("${app.ai.agentcli.command:claude -p}") String command,
            @Value("${app.ai.agentcli.system-flag:--append-system-prompt}") String systemFlag,
            @Value("${app.ai.agentcli.timeout-seconds:120}") int timeoutSeconds) {
        this.baseCommand = List.of(command.trim().split("\\s+"));
        this.systemFlag = systemFlag;
        this.timeoutSeconds = timeoutSeconds;
    }

    @Override
    public LlmResponse complete(LlmRequest request) {
        List<String> args = new ArrayList<>(baseCommand);
        if (request.system() != null && !request.system().isBlank()) {
            args.add(systemFlag);
            args.add(request.system());
        }

        Process process = null;
        try {
            process = new ProcessBuilder(args).start();

            // 프롬프트를 stdin으로 전달 (프로세스가 stdin을 이미 닫았을 수 있어 IOException 무시)
            try (OutputStream stdin = process.getOutputStream()) {
                stdin.write((request.prompt() == null ? "" : request.prompt())
                        .getBytes(StandardCharsets.UTF_8));
            } catch (IOException ignored) {
                // 프로세스가 stdin을 받지 않는 경우
            }

            boolean finished = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                throw new LlmTimeoutException("agentcli 호출 타임아웃(" + timeoutSeconds + "s)", null);
            }
            int exit = process.exitValue();
            if (exit != 0) {
                String err = readStream(process, false);
                throw new LlmApiException("agentcli 비정상 종료 exit=" + exit + " : " + err, null);
            }
            // 출력은 LLM 응답(≤수 KB)이므로 종료 후 일괄 읽어도 파이프 버퍼 한계 내
            return new LlmResponse(readStream(process, true));

        } catch (LlmTimeoutException | LlmApiException e) {
            throw e;
        } catch (InterruptedException e) {
            destroy(process);
            Thread.currentThread().interrupt();
            throw new LlmApiException("agentcli 대기 중 인터럽트", e);
        } catch (Exception e) {
            destroy(process);
            throw new LlmApiException("agentcli 실행 오류: " + e.getMessage(), e);
        }
    }

    private String readStream(Process process, boolean stdout) {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                stdout ? process.getInputStream() : process.getErrorStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append('\n');
            }
        } catch (IOException e) {
            log.warn("agentcli 스트림 읽기 실패: {}", e.getMessage());
        }
        return sb.toString().strip();
    }

    private void destroy(Process process) {
        if (process != null) {
            process.destroyForcibly();
        }
    }
}
```

- [ ] **Step 4: 테스트 통과 확인**

Run: `./gradlew test --tests "com.ggteam.cs.external.AgentCliClientTest"`
Expected: PASS (3건).

- [ ] **Step 5: 커밋**

```bash
git add backend/src/main/java/com/ggteam/cs/external/AgentCliClient.java \
        backend/src/test/java/com/ggteam/cs/external/AgentCliClientTest.java
git commit -m "feat(sim): agentcli(claude -p) 기반 LlmClient 추가"
```

---

### Task 2: SimScenario + ScenarioAssigner (가중 시나리오 분배)

**Files:**
- Create: `backend/src/main/java/com/ggteam/cs/sim/SimScenario.java`
- Create: `backend/src/main/java/com/ggteam/cs/sim/SimAssignment.java`
- Create: `backend/src/main/java/com/ggteam/cs/sim/ScenarioAssigner.java`
- Test: `backend/src/test/java/com/ggteam/cs/sim/ScenarioAssignerTest.java`

**Interfaces:**
- Produces:
  - `enum SimScenario { PAID_NOT_DELIVERED, DUPLICATE_CHARGE, PAYMENT_FAILED, PARTIAL_DELIVERY, REFUND_PENDING, POINT_NOT_CHARGED, WRONG_ITEM, ACCOUNT_ISSUE, ETC }` (각 `int weight()`)
  - `record SimAssignment(String userId, SimScenario scenario, int index)`
  - `ScenarioAssigner.assign(int count) : List<SimAssignment>` (결정적, userId=`sim-user-%04d`)

- [ ] **Step 1: 실패 테스트 작성**

```java
package com.ggteam.cs.sim;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class ScenarioAssignerTest {

    private final ScenarioAssigner assigner = new ScenarioAssigner();

    @Test
    void count만큼_결정적으로_분배한다() {
        List<SimAssignment> a = assigner.assign(100);

        assertThat(a).hasSize(100);
        assertThat(a.get(0).userId()).isEqualTo("sim-user-0001");
        assertThat(a.get(99).userId()).isEqualTo("sim-user-0100");
        assertThat(a.stream().map(SimAssignment::userId).distinct().count()).isEqualTo(100);
    }

    @Test
    void 가중치대로_분포한다() {
        var counts = assigner.assign(100).stream()
                .collect(Collectors.groupingBy(SimAssignment::scenario, Collectors.counting()));

        assertThat(counts.get(SimScenario.PAID_NOT_DELIVERED)).isEqualTo(25L);
        assertThat(counts.get(SimScenario.DUPLICATE_CHARGE)).isEqualTo(15L);
        assertThat(counts.get(SimScenario.ACCOUNT_ISSUE)).isEqualTo(10L);
        assertThat(counts.values().stream().mapToLong(Long::longValue).sum()).isEqualTo(100L);
    }
}
```

- [ ] **Step 2: 테스트 실패 확인**

Run: `./gradlew test --tests "com.ggteam.cs.sim.ScenarioAssignerTest"`
Expected: 컴파일 실패.

- [ ] **Step 3: 구현 작성**

`SimScenario.java`:
```java
package com.ggteam.cs.sim;

/** 시뮬레이션 시나리오. weight 합 = 100. */
public enum SimScenario {
    PAID_NOT_DELIVERED(25),
    DUPLICATE_CHARGE(15),
    PAYMENT_FAILED(12),
    PARTIAL_DELIVERY(10),
    REFUND_PENDING(10),
    POINT_NOT_CHARGED(8),
    WRONG_ITEM(5),
    ACCOUNT_ISSUE(10),
    ETC(5);

    private final int weight;

    SimScenario(int weight) {
        this.weight = weight;
    }

    public int weight() {
        return weight;
    }
}
```

`SimAssignment.java`:
```java
package com.ggteam.cs.sim;

/** userId ↔ 시나리오 배정 (시더·카탈로그 공유 단일 진실원). */
public record SimAssignment(String userId, SimScenario scenario, int index) {}
```

`ScenarioAssigner.java`:
```java
package com.ggteam.cs.sim;

import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * 가중치 기반 시나리오 분배기. 결정적(난수 없음)으로 userId↔시나리오를 배정한다.
 * weight 합 100을 count로 스케일하고 반올림 오차는 PAID_NOT_DELIVERED로 보정한다.
 */
@Component
public class ScenarioAssigner {

    public List<SimAssignment> assign(int count) {
        List<SimScenario> deck = new ArrayList<>();
        for (SimScenario s : SimScenario.values()) {
            int n = Math.round(s.weight() * count / 100f);
            for (int i = 0; i < n; i++) {
                deck.add(s);
            }
        }
        while (deck.size() < count) {
            deck.add(SimScenario.PAID_NOT_DELIVERED);
        }
        while (deck.size() > count) {
            deck.remove(deck.size() - 1);
        }

        List<SimAssignment> out = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            out.add(new SimAssignment(String.format("sim-user-%04d", i + 1), deck.get(i), i));
        }
        return out;
    }
}
```

- [ ] **Step 4: 테스트 통과 확인**

Run: `./gradlew test --tests "com.ggteam.cs.sim.ScenarioAssignerTest"`
Expected: PASS (2건).

- [ ] **Step 5: 커밋**

```bash
git add backend/src/main/java/com/ggteam/cs/sim/SimScenario.java \
        backend/src/main/java/com/ggteam/cs/sim/SimAssignment.java \
        backend/src/main/java/com/ggteam/cs/sim/ScenarioAssigner.java \
        backend/src/test/java/com/ggteam/cs/sim/ScenarioAssignerTest.java
git commit -m "feat(sim): 가중 시나리오 분배기(ScenarioAssigner) 추가"
```

---

### Task 3: PlannedInquiry + InquiryScenarioCatalog (문의 100건 생성)

**Files:**
- Create: `backend/src/main/java/com/ggteam/cs/sim/PlannedInquiry.java`
- Create: `backend/src/main/java/com/ggteam/cs/sim/InquiryScenarioCatalog.java`
- Test: `backend/src/test/java/com/ggteam/cs/sim/InquiryScenarioCatalogTest.java`

**Interfaces:**
- Consumes: `ScenarioAssigner.assign(int)`, `SimAssignment`, `SimScenario`, `InquiryType`(기존 enum: PAYMENT, ITEM_DELIVERY, ACCOUNT, ETC).
- Produces:
  - `record PlannedInquiry(String userId, InquiryType type, String content, SimScenario scenario)`
  - `InquiryScenarioCatalog.build(int count) : List<PlannedInquiry>`

- [ ] **Step 1: 실패 테스트 작성**

```java
package com.ggteam.cs.sim;

import static org.assertj.core.api.Assertions.assertThat;

import com.ggteam.cs.common.enums.InquiryType;
import java.util.List;
import org.junit.jupiter.api.Test;

class InquiryScenarioCatalogTest {

    private final InquiryScenarioCatalog catalog = new InquiryScenarioCatalog(new ScenarioAssigner());

    @Test
    void count건의_문의를_생성한다() {
        List<PlannedInquiry> plan = catalog.build(100);

        assertThat(plan).hasSize(100);
        assertThat(plan.stream().map(PlannedInquiry::userId).distinct().count()).isEqualTo(100);
    }

    @Test
    void 모든_본문은_10자_이상이다() {
        for (PlannedInquiry p : catalog.build(100)) {
            assertThat(p.content().length()).isGreaterThanOrEqualTo(10);
        }
    }

    @Test
    void 시나리오에_맞는_유형으로_매핑된다() {
        List<PlannedInquiry> plan = catalog.build(100);

        assertThat(plan).filteredOn(p -> p.scenario() == SimScenario.ACCOUNT_ISSUE)
                .allMatch(p -> p.type() == InquiryType.ACCOUNT);
        assertThat(plan).filteredOn(p -> p.scenario() == SimScenario.ETC)
                .allMatch(p -> p.type() == InquiryType.ETC);
        assertThat(plan).filteredOn(p -> p.scenario() == SimScenario.PAID_NOT_DELIVERED)
                .allMatch(p -> p.type() == InquiryType.PAYMENT);
    }
}
```

- [ ] **Step 2: 테스트 실패 확인**

Run: `./gradlew test --tests "com.ggteam.cs.sim.InquiryScenarioCatalogTest"`
Expected: 컴파일 실패.

- [ ] **Step 3: 구현 작성**

`PlannedInquiry.java`:
```java
package com.ggteam.cs.sim;

import com.ggteam.cs.common.enums.InquiryType;

/** 드립으로 주입할 사전 작성 문의 1건. */
public record PlannedInquiry(String userId, InquiryType type, String content, SimScenario scenario) {}
```

`InquiryScenarioCatalog.java`:
```java
package com.ggteam.cs.sim;

import com.ggteam.cs.common.enums.InquiryType;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * 시나리오 배정을 받아 정합되는 CS 문의 본문을 생성한다.
 * 본문은 시나리오별 템플릿을 index로 변주(결정적)하여 단조로움을 피한다.
 */
@Component
public class InquiryScenarioCatalog {

    private final ScenarioAssigner assigner;
    private final Map<SimScenario, String[]> templates = templates();

    public InquiryScenarioCatalog(ScenarioAssigner assigner) {
        this.assigner = assigner;
    }

    public List<PlannedInquiry> build(int count) {
        List<PlannedInquiry> out = new ArrayList<>(count);
        for (SimAssignment a : assigner.assign(count)) {
            String[] t = templates.get(a.scenario());
            out.add(new PlannedInquiry(a.userId(), typeOf(a.scenario()),
                    t[a.index() % t.length], a.scenario()));
        }
        return out;
    }

    private InquiryType typeOf(SimScenario s) {
        return switch (s) {
            case ACCOUNT_ISSUE -> InquiryType.ACCOUNT;
            case ETC -> InquiryType.ETC;
            default -> InquiryType.PAYMENT;
        };
    }

    private Map<SimScenario, String[]> templates() {
        Map<SimScenario, String[]> m = new EnumMap<>(SimScenario.class);
        m.put(SimScenario.PAID_NOT_DELIVERED, new String[]{
                "결제는 완료됐는데 아이템이 들어오지 않았어요. 확인 부탁드립니다.",
                "방금 결제했는데 아이템이 아직 지급되지 않았습니다. 빠른 처리 부탁해요.",
                "구매 완료 메시지는 떴는데 인벤토리에 아이템이 없어요.",
                "결제 성공했는데 한 시간째 아이템이 안 와요. 어떻게 된 건가요?"});
        m.put(SimScenario.DUPLICATE_CHARGE, new String[]{
                "같은 상품이 두 번 결제됐어요. 한 건 환불해주세요.",
                "결제가 중복으로 두 번 청구되었습니다. 확인 후 환불 바랍니다.",
                "동일 주문이 2회 결제된 것 같아요. 중복분 취소 부탁드립니다."});
        m.put(SimScenario.PAYMENT_FAILED, new String[]{
                "결제 실패라고 떴는데 카드에서는 돈이 빠져나갔어요.",
                "한도 초과로 실패했다는데 결제 문자가 왔습니다. 확인해주세요.",
                "결제가 안 됐다고 나오는데 출금은 됐어요. 어떻게 처리되나요?"});
        m.put(SimScenario.PARTIAL_DELIVERY, new String[]{
                "패키지를 샀는데 일부 아이템만 지급됐어요.",
                "묶음 상품 중 절반만 들어왔습니다. 나머지는 언제 오나요?",
                "구성품 일부가 지급되지 않았어요. 누락분 확인 부탁드립니다."});
        m.put(SimScenario.REFUND_PENDING, new String[]{
                "환불 신청한 지 며칠 됐는데 아직 처리가 안 됐어요.",
                "환불 요청했는데 진행 상황을 알 수 없습니다. 확인해주세요.",
                "환불이 접수만 되고 입금이 안 됐어요. 언제 처리되나요?"});
        m.put(SimScenario.POINT_NOT_CHARGED, new String[]{
                "포인트 충전 결제는 됐는데 포인트가 안 들어왔어요.",
                "캐시 충전했는데 잔액에 반영이 안 됩니다. 확인 부탁해요.",
                "포인트 결제 후에도 잔액이 그대로예요. 충전이 누락된 것 같아요."});
        m.put(SimScenario.WRONG_ITEM, new String[]{
                "주문한 아이템과 다른 아이템이 지급됐어요.",
                "구매한 것과 전혀 다른 상품이 들어왔습니다. 교환해주세요.",
                "엉뚱한 아이템이 지급됐어요. 올바른 아이템으로 변경 부탁드립니다."});
        m.put(SimScenario.ACCOUNT_ISSUE, new String[]{
                "갑자기 로그인이 안 됩니다. 계정에 문제가 있나요?",
                "계정이 정지된 것 같아요. 사유와 해제 방법을 알려주세요.",
                "휴면 계정이라고 떠서 접속이 안 됩니다. 복구 부탁드립니다."});
        m.put(SimScenario.ETC, new String[]{
                "게임 이용 관련해서 문의드릴 게 있습니다. 답변 부탁드려요.",
                "서비스 이용 중 궁금한 점이 있어 문의 남깁니다.",
                "일반 문의입니다. 확인 후 안내 부탁드립니다."});
        return m;
    }
}
```

- [ ] **Step 4: 테스트 통과 확인**

Run: `./gradlew test --tests "com.ggteam.cs.sim.InquiryScenarioCatalogTest"`
Expected: PASS (3건).

- [ ] **Step 5: 커밋**

```bash
git add backend/src/main/java/com/ggteam/cs/sim/PlannedInquiry.java \
        backend/src/main/java/com/ggteam/cs/sim/InquiryScenarioCatalog.java \
        backend/src/test/java/com/ggteam/cs/sim/InquiryScenarioCatalogTest.java
git commit -m "feat(sim): 시나리오 정합 문의 카탈로그(InquiryScenarioCatalog) 추가"
```

---

### Task 4: SimDataSeeder (계정/결제/아이템 시드)

**Files:**
- Create: `backend/src/main/java/com/ggteam/cs/sim/SimDataSeeder.java`
- Test: `backend/src/test/java/com/ggteam/cs/sim/SimDataSeederTest.java`

**Interfaces:**
- Consumes: `ScenarioAssigner`, `SimAssignment`, `SimScenario`, `AccountRepository`, `PaymentRepository`, `ItemDeliveryRepository`, 엔티티 `Account/Payment/ItemDelivery`(setter 기반), enum `DemoEnums.{AccountStatus,PaymentStatus,DeliveryStatus}`, `SimProperties`(Task 5에서 생성 — 본 task 런타임만 사용, 테스트는 `seed(...)`를 직접 호출).
- Produces:
  - `record SimDataSeeder.SeedSummary(int accounts, int payments, int items)`
  - `SimDataSeeder.seed(List<SimAssignment> assignments, int itemTarget) : SeedSummary` (public, 테스트 대상)

> NOTE: 본 task는 `SimProperties`를 주입하지만 `SimProperties`는 Task 5에서 생성한다. 컴파일 순서를 위해, 먼저 아래 최소 스텁을 만든 뒤 Task 5에서 본체를 완성한다.
> `backend/src/main/java/com/ggteam/cs/sim/SimProperties.java`:
> ```java
> package com.ggteam.cs.sim;
> import org.springframework.boot.context.properties.ConfigurationProperties;
> import org.springframework.context.annotation.Profile;
> import org.springframework.stereotype.Component;
>
> @Component
> @Profile("sim")
> @ConfigurationProperties(prefix = "app.sim")
> public class SimProperties {
>     private int count = 100;
>     private int itemTarget = 500;
>     private int durationMinutes = 20;
>     private boolean jitter = false;
>     private boolean autoStart = false;
>     public int getCount() { return count; }
>     public void setCount(int v) { this.count = v; }
>     public int getItemTarget() { return itemTarget; }
>     public void setItemTarget(int v) { this.itemTarget = v; }
>     public int getDurationMinutes() { return durationMinutes; }
>     public void setDurationMinutes(int v) { this.durationMinutes = v; }
>     public boolean isJitter() { return jitter; }
>     public void setJitter(boolean v) { this.jitter = v; }
>     public boolean isAutoStart() { return autoStart; }
>     public void setAutoStart(boolean v) { this.autoStart = v; }
> }
> ```

- [ ] **Step 1: 실패 테스트 작성**

```java
package com.ggteam.cs.sim;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.ggteam.cs.persistence.repository.AccountRepository;
import com.ggteam.cs.persistence.repository.ItemDeliveryRepository;
import com.ggteam.cs.persistence.repository.PaymentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SimDataSeederTest {

    @Mock AccountRepository accountRepo;
    @Mock PaymentRepository paymentRepo;
    @Mock ItemDeliveryRepository itemRepo;

    @Test
    void 계정100_결제100_아이템500을_시드한다() {
        when(accountRepo.save(any())).then(returnsFirstArg());
        when(paymentRepo.save(any())).then(returnsFirstArg());
        when(itemRepo.save(any())).then(returnsFirstArg());

        SimDataSeeder seeder = new SimDataSeeder(
                new ScenarioAssigner(), new SimProperties(), accountRepo, paymentRepo, itemRepo);

        SimDataSeeder.SeedSummary s = seeder.seed(new ScenarioAssigner().assign(100), 500);

        assertThat(s.accounts()).isEqualTo(100);
        assertThat(s.payments()).isEqualTo(100);
        assertThat(s.items()).isEqualTo(500);
    }
}
```

- [ ] **Step 2: 테스트 실패 확인**

Run: `./gradlew test --tests "com.ggteam.cs.sim.SimDataSeederTest"`
Expected: 컴파일 실패 (`SimDataSeeder` 없음). `SimProperties` 스텁이 없으면 함께 생성.

- [ ] **Step 3: 구현 작성**

```java
package com.ggteam.cs.sim;

import com.ggteam.cs.common.enums.DemoEnums.AccountStatus;
import com.ggteam.cs.common.enums.DemoEnums.DeliveryStatus;
import com.ggteam.cs.common.enums.DemoEnums.PaymentStatus;
import com.ggteam.cs.persistence.entity.Account;
import com.ggteam.cs.persistence.entity.ItemDelivery;
import com.ggteam.cs.persistence.entity.Payment;
import com.ggteam.cs.persistence.repository.AccountRepository;
import com.ggteam.cs.persistence.repository.ItemDeliveryRepository;
import com.ggteam.cs.persistence.repository.PaymentRepository;
import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * sim 프로파일 데이터 시더. 시나리오 배정에 따라 계정/결제/아이템지급을 정합되게 시드한다.
 * 총 지급건수는 itemTarget까지 정상지급(DELIVERED) 이력으로 패딩한다.
 */
@Component
@Profile("sim")
@Order(1)
public class SimDataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(SimDataSeeder.class);

    private final ScenarioAssigner assigner;
    private final SimProperties props;
    private final AccountRepository accountRepo;
    private final PaymentRepository paymentRepo;
    private final ItemDeliveryRepository itemRepo;

    public SimDataSeeder(ScenarioAssigner assigner, SimProperties props,
                         AccountRepository accountRepo, PaymentRepository paymentRepo,
                         ItemDeliveryRepository itemRepo) {
        this.assigner = assigner;
        this.props = props;
        this.accountRepo = accountRepo;
        this.paymentRepo = paymentRepo;
        this.itemRepo = itemRepo;
    }

    @Override
    @Transactional
    public void run(String... args) {
        SeedSummary s = seed(assigner.assign(props.getCount()), props.getItemTarget());
        log.info("========== [SIM] 시드 완료 ==========");
        log.info("[SIM] accounts={} payments={} items={}", s.accounts(), s.payments(), s.items());
        log.info("[SIM] 제어판: http://localhost:5173/dev/sim  (드립 시작은 제어판에서)");
        log.info("=====================================");
    }

    /** 시드 핵심 로직 (테스트 대상). counters[0]=payments, counters[1]=items. */
    public SeedSummary seed(List<SimAssignment> assignments, int itemTarget) {
        int[] c = {0, 0};
        for (SimAssignment a : assignments) {
            accountRepo.save(account(a));
            seedScenario(a, c);
        }
        // 총 지급건수를 itemTarget까지 정상지급 이력으로 패딩 (참조 결제 없음)
        int i = 0;
        while (c[1] < itemTarget && !assignments.isEmpty()) {
            SimAssignment a = assignments.get(i % assignments.size());
            saveDelivery(a.userId(), null, DeliveryStatus.DELIVERED, "item-hist-" + c[1], c);
            i++;
        }
        return new SeedSummary(assignments.size(), c[0], c[1]);
    }

    private void seedScenario(SimAssignment a, int[] c) {
        String u = a.userId();
        switch (a.scenario()) {
            case PAID_NOT_DELIVERED -> {
                UUID pid = savePayment(u, "9900", PaymentStatus.SUCCESS, null, c);
                saveDelivery(u, pid, DeliveryStatus.NOT_DELIVERED, "item-sword-100", c);
            }
            case DUPLICATE_CHARGE -> {
                UUID p1 = savePayment(u, "9900", PaymentStatus.SUCCESS, null, c);
                savePayment(u, "9900", PaymentStatus.SUCCESS, null, c);
                saveDelivery(u, p1, DeliveryStatus.DELIVERED, "item-gold-500", c);
            }
            case PAYMENT_FAILED ->
                    savePayment(u, "4900", PaymentStatus.FAILED, "CARD_LIMIT_EXCEEDED: 카드 한도 초과", c);
            case PARTIAL_DELIVERY -> {
                UUID pid = savePayment(u, "19900", PaymentStatus.SUCCESS, null, c);
                saveDelivery(u, pid, DeliveryStatus.PARTIAL, "item-bundle-10", c);
            }
            case REFUND_PENDING -> savePayment(u, "9900", PaymentStatus.REFUNDED, null, c);
            case POINT_NOT_CHARGED -> {
                UUID pid = savePayment(u, "5000", PaymentStatus.SUCCESS, null, c);
                saveDelivery(u, pid, DeliveryStatus.NOT_DELIVERED, "item-point-5000", c);
            }
            case WRONG_ITEM -> {
                UUID pid = savePayment(u, "9900", PaymentStatus.SUCCESS, null, c);
                saveDelivery(u, pid, DeliveryStatus.DELIVERED, "item-wrong-999", c);
            }
            case ACCOUNT_ISSUE, ETC -> {
                // 결제 이력 없음 (계정 상태/일반 문의로 표현)
            }
        }
    }

    private Account account(SimAssignment a) {
        Account acc = new Account();
        acc.setUserId(a.userId());
        acc.setStatus(a.scenario() == SimScenario.ACCOUNT_ISSUE
                ? switch (a.index() % 3) {
                    case 0 -> AccountStatus.SUSPENDED;
                    case 1 -> AccountStatus.DORMANT;
                    default -> AccountStatus.BANNED;
                }
                : AccountStatus.ACTIVE);
        acc.setLastLogin(ZonedDateTime.now());
        return acc;
    }

    private UUID savePayment(String userId, String amount, PaymentStatus status, String errorLog, int[] c) {
        Payment p = new Payment();
        p.setUserId(userId);
        p.setAmount(new BigDecimal(amount));
        p.setStatus(status);
        p.setErrorLog(errorLog);
        Payment saved = paymentRepo.save(p);
        c[0]++;
        return saved.getId();
    }

    private void saveDelivery(String userId, UUID paymentId, DeliveryStatus status, String itemId, int[] c) {
        ItemDelivery d = new ItemDelivery();
        d.setPaymentId(paymentId);
        d.setUserId(userId);
        d.setItemId(itemId);
        d.setStatus(status);
        itemRepo.save(d);
        c[1]++;
    }

    public record SeedSummary(int accounts, int payments, int items) {}
}
```

- [ ] **Step 4: 테스트 통과 확인**

Run: `./gradlew test --tests "com.ggteam.cs.sim.SimDataSeederTest"`
Expected: PASS. (payments=100: PAID25+DUP30+FAILED12+PARTIAL10+REFUND10+POINT8+WRONG5; items 패딩 후 500)

- [ ] **Step 5: 커밋**

```bash
git add backend/src/main/java/com/ggteam/cs/sim/SimDataSeeder.java \
        backend/src/main/java/com/ggteam/cs/sim/SimProperties.java \
        backend/src/test/java/com/ggteam/cs/sim/SimDataSeederTest.java
git commit -m "feat(sim): 계정/결제/아이템 정합 시더(SimDataSeeder) 추가"
```

---

### Task 5: 설정 — SimProperties 완성 + application-sim.yml + CORS

**Files:**
- Modify/확정: `backend/src/main/java/com/ggteam/cs/sim/SimProperties.java` (Task 4 스텁이 곧 최종본 — 변경 없으면 그대로)
- Create: `backend/src/main/java/com/ggteam/cs/sim/SimWebConfig.java`
- Create: `backend/src/main/resources/application-sim.yml`
- Test: `backend/src/test/java/com/ggteam/cs/sim/SimPropertiesTest.java`

**Interfaces:**
- Produces: `SimWebConfig`(@Profile("sim") WebMvcConfigurer, `/api/v1/**` CORS 허용), `application-sim.yml`(H2/보안제외/agentcli/풀/app.sim).

- [ ] **Step 1: 실패 테스트 작성**

```java
package com.ggteam.cs.sim;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class SimPropertiesTest {

    @Test
    void 기본값() {
        SimProperties p = new SimProperties();

        assertThat(p.getCount()).isEqualTo(100);
        assertThat(p.getItemTarget()).isEqualTo(500);
        assertThat(p.getDurationMinutes()).isEqualTo(20);
        assertThat(p.isJitter()).isFalse();
        assertThat(p.isAutoStart()).isFalse();
    }
}
```

- [ ] **Step 2: 테스트 실패/통과 확인**

Run: `./gradlew test --tests "com.ggteam.cs.sim.SimPropertiesTest"`
Expected: Task 4의 `SimProperties`가 이미 있으면 PASS. (없으면 위 NOTE의 본체 생성)

- [ ] **Step 3: SimWebConfig 작성**

```java
package com.ggteam.cs.sim;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** sim 프로파일 전용 CORS. 프론트 제어판(Vite dev)이 실 백엔드를 직접 호출하도록 허용. */
@Configuration
@Profile("sim")
public class SimWebConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/v1/**")
                .allowedOrigins("http://localhost:5173", "http://localhost:5174", "http://localhost:3000")
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                .allowedHeaders("*");
    }
}
```

- [ ] **Step 4: application-sim.yml 작성**

```yaml
# 트래픽 시뮬레이터 프로파일. local과 동일하게 자급(H2/보안제외)하되 agentcli + 시드/드립 활성.
#   실행: ./gradlew bootRun --args='--spring.profiles.active=sim'
spring:
  config:
    activate:
      on-profile: sim
  autoconfigure:
    exclude:
      - org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration
      - org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration
  datasource:
    url: jdbc:h2:mem:csagent-sim;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE
    driver-class-name: org.h2.Driver
    username: sa
    password: ""
  jpa:
    hibernate:
      ddl-auto: create-drop
    properties:
      hibernate:
        jdbc:
          time_zone: Asia/Seoul
  flyway:
    enabled: false
  h2:
    console:
      enabled: true
      path: /h2-console
  task:
    execution:
      pool:
        core-size: 3
        max-size: 4
        queue-capacity: 200

app:
  ai:
    llm-client: agentcli          # mock 으로 바꾸면 즉시·무료 데모
    agentcli:
      command: "claude -p"
      system-flag: "--append-system-prompt"
      timeout-seconds: 120
  sim:
    count: 100
    item-target: 500
    duration-minutes: 20
    jitter: false
    auto-start: false
```

- [ ] **Step 5: 전체 백엔드 빌드 확인**

Run: `./gradlew test`
Expected: 전 테스트 PASS (sim 빈은 sim 프로파일에서만 로드되어 일반 테스트 컨텍스트 영향 없음).

- [ ] **Step 6: 커밋**

```bash
git add backend/src/main/java/com/ggteam/cs/sim/SimProperties.java \
        backend/src/main/java/com/ggteam/cs/sim/SimWebConfig.java \
        backend/src/main/resources/application-sim.yml \
        backend/src/test/java/com/ggteam/cs/sim/SimPropertiesTest.java
git commit -m "feat(sim): sim 프로파일 설정(application-sim.yml)·CORS·SimProperties"
```

---

### Task 6: SimulationStatus + InquirySender + SimulationService (드립 엔진)

**Files:**
- Create: `backend/src/main/java/com/ggteam/cs/sim/SimulationStatus.java`
- Create: `backend/src/main/java/com/ggteam/cs/sim/InquirySender.java`
- Create: `backend/src/main/java/com/ggteam/cs/sim/SimulationService.java`
- Test: `backend/src/test/java/com/ggteam/cs/sim/SimulationServiceTest.java`

**Interfaces:**
- Consumes: `InquiryScenarioCatalog.build(int)`, `PlannedInquiry`, `SimProperties`.
- Produces:
  - `record SimulationStatus(boolean running, int total, int sent, int errors, Long startedAtEpochMs, long elapsedSeconds, long etaSeconds, double ratePerMin, String llmClient)`
  - `interface InquirySender { void send(PlannedInquiry inquiry); }`
  - `SimulationService` (@Service @Profile("sim")):
    - `SimulationStatus start(Integer count, Integer durationMinutes, Boolean jitter)`
    - package-private `SimulationStatus startInternal(int count, long durationMs, boolean jitter)` (테스트용)
    - `SimulationStatus stop()`, `SimulationStatus reset()`, `SimulationStatus status()`

- [ ] **Step 1: 실패 테스트 작성**

```java
package com.ggteam.cs.sim;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;

class SimulationServiceTest {

    private SimulationService newService(InquirySender sender) {
        InquiryScenarioCatalog catalog = new InquiryScenarioCatalog(new ScenarioAssigner());
        return new SimulationService(catalog, sender, new SimProperties(), "mock");
    }

    private void awaitSent(SimulationService svc, int target) throws InterruptedException {
        long deadline = System.currentTimeMillis() + 3000;
        while (svc.status().sent() < target && System.currentTimeMillis() < deadline) {
            Thread.sleep(10);
        }
    }

    @Test
    void 짧은_윈도우에_전건을_전송한다() throws InterruptedException {
        List<PlannedInquiry> recorded = Collections.synchronizedList(new ArrayList<>());
        SimulationService svc = newService(recorded::add);

        svc.startInternal(3, 60, false);
        awaitSent(svc, 3);

        assertThat(svc.status().sent()).isEqualTo(3);
        assertThat(svc.status().total()).isEqualTo(3);
        assertThat(recorded).hasSize(3);
    }

    @Test
    void stop_후_running은_false() throws InterruptedException {
        SimulationService svc = newService(p -> { });

        svc.startInternal(50, 60_000, false); // 긴 간격
        SimulationStatus after = svc.stop();

        assertThat(after.running()).isFalse();
        assertThat(after.sent()).isLessThan(50);
    }
}
```

- [ ] **Step 2: 테스트 실패 확인**

Run: `./gradlew test --tests "com.ggteam.cs.sim.SimulationServiceTest"`
Expected: 컴파일 실패.

- [ ] **Step 3: 구현 작성**

`SimulationStatus.java`:
```java
package com.ggteam.cs.sim;

/** 시뮬레이션 진행 상태 스냅샷. */
public record SimulationStatus(
        boolean running,
        int total,
        int sent,
        int errors,
        Long startedAtEpochMs,
        long elapsedSeconds,
        long etaSeconds,
        double ratePerMin,
        String llmClient) {}
```

`InquirySender.java`:
```java
package com.ggteam.cs.sim;

/** 계획된 문의 1건을 실제 접수 경로로 전송한다. */
public interface InquirySender {
    void send(PlannedInquiry inquiry);
}
```

`SimulationService.java`:
```java
package com.ggteam.cs.sim;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

/**
 * 드립 엔진. duration/count로 간격을 계산해 ScheduledExecutorService로 한 건씩 전송한다.
 * 진행 상태(running/total/sent/errors)를 보유하고 start/stop/reset/status를 제공한다.
 */
@Service
@Profile("sim")
public class SimulationService {

    private static final Logger log = LoggerFactory.getLogger(SimulationService.class);

    private final InquiryScenarioCatalog catalog;
    private final InquirySender sender;
    private final SimProperties props;
    private final String llmClient;

    private final AtomicInteger sent = new AtomicInteger();
    private final AtomicInteger errors = new AtomicInteger();
    private volatile int total = 0;
    private volatile boolean running = false;
    private volatile Long startedAtMs = null;
    private ScheduledExecutorService scheduler;

    public SimulationService(InquiryScenarioCatalog catalog, InquirySender sender, SimProperties props,
                             @Value("${app.ai.llm-client:agentcli}") String llmClient) {
        this.catalog = catalog;
        this.sender = sender;
        this.props = props;
        this.llmClient = llmClient;
    }

    public synchronized SimulationStatus start(Integer count, Integer durationMinutes, Boolean jitter) {
        int n = count != null ? count : props.getCount();
        long durationMs = (durationMinutes != null ? durationMinutes : props.getDurationMinutes()) * 60_000L;
        boolean useJitter = jitter != null ? jitter : props.isJitter();
        return startInternal(n, durationMs, useJitter);
    }

    synchronized SimulationStatus startInternal(int count, long durationMs, boolean jitter) {
        if (running) {
            return status();
        }
        List<PlannedInquiry> plan = catalog.build(count);
        total = plan.size();
        sent.set(0);
        errors.set(0);
        running = true;
        startedAtMs = System.currentTimeMillis();

        long interval = total > 0 ? Math.max(1, durationMs / total) : durationMs;
        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "sim-drip");
            t.setDaemon(true);
            return t;
        });

        for (int i = 0; i < total; i++) {
            final PlannedInquiry inq = plan.get(i);
            long base = i * interval;
            long delay = jitter ? Math.max(0, base + ((i * 9301L + 49297L) % interval) - interval / 2) : base;
            scheduler.schedule(() -> dispatch(inq), delay, TimeUnit.MILLISECONDS);
        }
        scheduler.schedule(() -> running = false, (long) total * interval + 50, TimeUnit.MILLISECONDS);

        log.info("[SIM] 드립 시작 total={} interval={}ms jitter={}", total, interval, jitter);
        return status();
    }

    private void dispatch(PlannedInquiry inq) {
        try {
            sender.send(inq);
            sent.incrementAndGet();
        } catch (Exception e) {
            errors.incrementAndGet();
            log.warn("[SIM] 전송 실패 {}: {}", inq.userId(), e.getMessage());
        }
    }

    public synchronized SimulationStatus stop() {
        running = false;
        if (scheduler != null) {
            scheduler.shutdownNow();
            scheduler = null;
        }
        return status();
    }

    public synchronized SimulationStatus reset() {
        stop();
        sent.set(0);
        errors.set(0);
        total = 0;
        startedAtMs = null;
        return status();
    }

    public SimulationStatus status() {
        long elapsed = startedAtMs == null ? 0 : Math.max(0, (System.currentTimeMillis() - startedAtMs) / 1000);
        int done = sent.get();
        double ratePerMin = elapsed > 0 ? done * 60.0 / elapsed : 0.0;
        long eta = ratePerMin > 0 ? (long) ((total - done) / (ratePerMin / 60.0)) : 0;
        return new SimulationStatus(running, total, done, errors.get(),
                startedAtMs, elapsed, Math.max(0, eta), ratePerMin, llmClient);
    }
}
```

- [ ] **Step 4: 테스트 통과 확인**

Run: `./gradlew test --tests "com.ggteam.cs.sim.SimulationServiceTest"`
Expected: PASS (2건).

- [ ] **Step 5: 커밋**

```bash
git add backend/src/main/java/com/ggteam/cs/sim/SimulationStatus.java \
        backend/src/main/java/com/ggteam/cs/sim/InquirySender.java \
        backend/src/main/java/com/ggteam/cs/sim/SimulationService.java \
        backend/src/test/java/com/ggteam/cs/sim/SimulationServiceTest.java
git commit -m "feat(sim): 드립 엔진(SimulationService)·InquirySender·SimulationStatus"
```

---

### Task 7: HttpInquirySender + SimulationController (REST 배선)

**Files:**
- Create: `backend/src/main/java/com/ggteam/cs/sim/HttpInquirySender.java`
- Create: `backend/src/main/java/com/ggteam/cs/sim/SimulationController.java`
- Test: `backend/src/test/java/com/ggteam/cs/sim/SimulationControllerTest.java`

**Interfaces:**
- Consumes: `SimulationService`(start/stop/reset/status), `PlannedInquiry`, `InquirySender`, `ApiResponse`(기존 `common/ApiResponse.java`, `ApiResponse.of(T)`).
- Produces:
  - `HttpInquirySender` (@Component @Profile("sim")) — `RestClient`로 `POST /api/v1/inquiries`.
  - `SimulationController` (@RestController @Profile("sim"), `/api/v1/dev/simulation/{start,stop,reset,status}`), `record StartRequest(Integer count, Integer durationMinutes, Boolean jitter)`.

- [ ] **Step 1: 실패 테스트 작성** (컨트롤러는 서비스 위임만 검증 — 순수 단위)

```java
package com.ggteam.cs.sim;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ggteam.cs.sim.SimulationController.StartRequest;
import org.junit.jupiter.api.Test;

class SimulationControllerTest {

    private final SimulationService service = mock(SimulationService.class);
    private final SimulationController controller = new SimulationController(service);

    private SimulationStatus sample() {
        return new SimulationStatus(true, 100, 0, 0, 123L, 0, 0, 0.0, "agentcli");
    }

    @Test
    void start는_파라미터를_서비스에_위임한다() {
        when(service.start(10, 5, true)).thenReturn(sample());

        var resp = controller.start(new StartRequest(10, 5, true));

        assertThat(resp.getStatusCode().value()).isEqualTo(200);
        verify(service).start(10, 5, true);
    }

    @Test
    void start_바디_null이면_기본값_위임() {
        when(service.start(null, null, null)).thenReturn(sample());

        controller.start(null);

        verify(service).start(null, null, null);
    }

    @Test
    void status_위임() {
        when(service.status()).thenReturn(sample());

        var resp = controller.status();

        assertThat(resp.getStatusCode().value()).isEqualTo(200);
        verify(service).status();
    }
}
```

- [ ] **Step 2: 테스트 실패 확인**

Run: `./gradlew test --tests "com.ggteam.cs.sim.SimulationControllerTest"`
Expected: 컴파일 실패.

- [ ] **Step 3: 구현 작성**

`HttpInquirySender.java`:
```java
package com.ggteam.cs.sim;

import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/** 자기 자신의 공개 접수 API(POST /api/v1/inquiries)를 호출해 실제 파이프라인을 태운다. */
@Component
@Profile("sim")
public class HttpInquirySender implements InquirySender {

    private final RestClient restClient;

    public HttpInquirySender(@Value("${server.port:8080}") int port) {
        this.restClient = RestClient.create("http://localhost:" + port);
    }

    @Override
    public void send(PlannedInquiry inq) {
        Map<String, Object> body = Map.of(
                "customerInfo", Map.of("userId", inq.userId(), "nickname", inq.userId() + "님", "channel", "WEB"),
                "customerType", inq.type().name(),
                "content", inq.content());
        restClient.post().uri("/api/v1/inquiries")
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .toBodilessEntity();
    }
}
```

`SimulationController.java`:
```java
package com.ggteam.cs.sim;

import com.ggteam.cs.common.ApiResponse;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 시뮬레이션 제어 API (sim 프로파일 전용).
 * POST start/stop/reset, GET status.
 */
@RestController
@RequestMapping("/api/v1/dev/simulation")
@Profile("sim")
public class SimulationController {

    private final SimulationService service;

    public SimulationController(SimulationService service) {
        this.service = service;
    }

    @PostMapping("/start")
    public ResponseEntity<ApiResponse<SimulationStatus>> start(
            @RequestBody(required = false) StartRequest req) {
        StartRequest r = req != null ? req : new StartRequest(null, null, null);
        return ResponseEntity.ok(ApiResponse.of(service.start(r.count(), r.durationMinutes(), r.jitter())));
    }

    @PostMapping("/stop")
    public ResponseEntity<ApiResponse<SimulationStatus>> stop() {
        return ResponseEntity.ok(ApiResponse.of(service.stop()));
    }

    @PostMapping("/reset")
    public ResponseEntity<ApiResponse<SimulationStatus>> reset() {
        return ResponseEntity.ok(ApiResponse.of(service.reset()));
    }

    @GetMapping("/status")
    public ResponseEntity<ApiResponse<SimulationStatus>> status() {
        return ResponseEntity.ok(ApiResponse.of(service.status()));
    }

    public record StartRequest(Integer count, Integer durationMinutes, Boolean jitter) {}
}
```

> 확인: `ApiResponse.of(...)`의 정확한 시그니처는 `backend/src/main/java/com/ggteam/cs/common/ApiResponse.java`에서 검증. 기존 `InquiryController`/`DashboardController`가 `ApiResponse.of(x)` 형태로 사용하므로 동일 패턴.

- [ ] **Step 4: 테스트 통과 확인**

Run: `./gradlew test --tests "com.ggteam.cs.sim.SimulationControllerTest"`
Expected: PASS (3건).

- [ ] **Step 5: 전체 백엔드 빌드**

Run: `./gradlew build -x test && ./gradlew test`
Expected: BUILD SUCCESSFUL, 전 테스트 PASS.

- [ ] **Step 6: 커밋**

```bash
git add backend/src/main/java/com/ggteam/cs/sim/HttpInquirySender.java \
        backend/src/main/java/com/ggteam/cs/sim/SimulationController.java \
        backend/src/test/java/com/ggteam/cs/sim/SimulationControllerTest.java
git commit -m "feat(sim): 자기호출 전송기(HttpInquirySender)·제어 API(SimulationController)"
```

---

### Task 8: 프론트 제어판 (simApi + SimulationControlPage + 라우트)

**Files:**
- Create: `frontend/src/dev/simApi.ts`
- Create: `frontend/src/dev/SimulationControlPage.tsx`
- Create: `frontend/src/dev/SimulationControlPage.test.tsx`
- Modify: `frontend/src/main.tsx` (라우트 `/dev/sim` 추가)

**Interfaces:**
- Consumes: 백엔드 `/api/v1/dev/simulation/{start,stop,reset,status}` (절대 URL, ApiResponse 래퍼 `{data:T}`).
- Produces: `startSimulation/stopSimulation/resetSimulation/fetchSimulationStatus` + `SimulationStatus`/`StartParams` 타입, `SimulationControlPage` 컴포넌트.

- [ ] **Step 1: 실패 테스트 작성**

`frontend/src/dev/SimulationControlPage.test.tsx`:
```tsx
import { render, screen, fireEvent, waitFor } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import * as simApi from "./simApi";
import { SimulationControlPage } from "./SimulationControlPage";

vi.mock("./simApi");

function status(running: boolean, sent: number): simApi.SimulationStatus {
  return {
    running,
    total: 100,
    sent,
    errors: 0,
    startedAtEpochMs: running ? 1 : null,
    elapsedSeconds: 0,
    etaSeconds: 0,
    ratePerMin: 0,
    llmClient: "agentcli",
  };
}

describe("SimulationControlPage", () => {
  beforeEach(() => {
    vi.mocked(simApi.fetchSimulationStatus).mockResolvedValue(status(false, 0));
    vi.mocked(simApi.startSimulation).mockResolvedValue(status(true, 0));
    vi.mocked(simApi.stopSimulation).mockResolvedValue(status(false, 0));
  });

  it("시작 버튼 클릭 시 startSimulation 호출", async () => {
    render(<SimulationControlPage />);
    fireEvent.click(await screen.findByRole("button", { name: /시작/ }));
    await waitFor(() => expect(simApi.startSimulation).toHaveBeenCalled());
  });

  it("진행 건수를 표시한다", async () => {
    vi.mocked(simApi.fetchSimulationStatus).mockResolvedValue(status(true, 42));
    render(<SimulationControlPage />);
    expect(await screen.findByText(/42\s*\/\s*100/)).toBeInTheDocument();
  });
});
```

- [ ] **Step 2: 테스트 실패 확인**

Run (frontend 디렉터리): `npm test -- --run src/dev/SimulationControlPage.test.tsx`
Expected: 실패 (모듈 없음).

- [ ] **Step 3: simApi.ts 작성**

```ts
// 시뮬레이션 제어 API — 실 백엔드를 절대 URL로 직접 호출(MSW 우회).
import axios from "axios";

const SIM_BASE =
  (import.meta.env.VITE_SIM_BASE as string | undefined) ??
  "http://localhost:8080/api/v1";

const simClient = axios.create({
  baseURL: SIM_BASE,
  headers: { "Content-Type": "application/json" },
});

export interface SimulationStatus {
  running: boolean;
  total: number;
  sent: number;
  errors: number;
  startedAtEpochMs: number | null;
  elapsedSeconds: number;
  etaSeconds: number;
  ratePerMin: number;
  llmClient: string;
}

export interface StartParams {
  count?: number;
  durationMinutes?: number;
  jitter?: boolean;
}

interface Envelope<T> {
  data: T;
}

export async function startSimulation(p: StartParams = {}): Promise<SimulationStatus> {
  const res = await simClient.post<Envelope<SimulationStatus>>("/dev/simulation/start", p);
  return res.data.data;
}

export async function stopSimulation(): Promise<SimulationStatus> {
  const res = await simClient.post<Envelope<SimulationStatus>>("/dev/simulation/stop");
  return res.data.data;
}

export async function resetSimulation(): Promise<SimulationStatus> {
  const res = await simClient.post<Envelope<SimulationStatus>>("/dev/simulation/reset");
  return res.data.data;
}

export async function fetchSimulationStatus(): Promise<SimulationStatus> {
  const res = await simClient.get<Envelope<SimulationStatus>>("/dev/simulation/status");
  return res.data.data;
}
```

- [ ] **Step 4: SimulationControlPage.tsx 작성**

```tsx
import { useCallback, useEffect, useState } from "react";
import {
  fetchSimulationStatus,
  resetSimulation,
  startSimulation,
  stopSimulation,
  type SimulationStatus,
} from "./simApi";

export function SimulationControlPage() {
  const [status, setStatus] = useState<SimulationStatus | null>(null);
  const [count, setCount] = useState(100);
  const [durationMinutes, setDurationMinutes] = useState(20);
  const [jitter, setJitter] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const refresh = useCallback(async () => {
    try {
      setStatus(await fetchSimulationStatus());
      setError(null);
    } catch {
      setError("백엔드(sim 프로파일, :8080)에 연결할 수 없습니다.");
    }
  }, []);

  useEffect(() => {
    refresh();
    const id = setInterval(refresh, 2000);
    return () => clearInterval(id);
  }, [refresh]);

  const onStart = async () => {
    try {
      setStatus(await startSimulation({ count, durationMinutes, jitter }));
    } catch {
      setError("시작 실패");
    }
  };
  const onStop = async () => setStatus(await stopSimulation());
  const onReset = async () => setStatus(await resetSimulation());

  const sent = status?.sent ?? 0;
  const total = status?.total ?? 0;
  const pct = total > 0 ? Math.round((sent / total) * 100) : 0;

  return (
    <div style={{ maxWidth: 720, margin: "40px auto", padding: 24, fontFamily: "sans-serif" }}>
      <h1>CS 트래픽 시뮬레이터</h1>
      {error && <p style={{ color: "crimson" }}>{error}</p>}

      <section style={{ display: "flex", gap: 16, flexWrap: "wrap", marginBottom: 24 }}>
        <label>
          건수
          <input type="number" value={count} onChange={(e) => setCount(Number(e.target.value))} />
        </label>
        <label>
          소요(분)
          <input
            type="number"
            value={durationMinutes}
            onChange={(e) => setDurationMinutes(Number(e.target.value))}
          />
        </label>
        <label>
          <input type="checkbox" checked={jitter} onChange={(e) => setJitter(e.target.checked)} />
          불규칙 도착(jitter)
        </label>
      </section>

      <section style={{ display: "flex", gap: 12, marginBottom: 24 }}>
        <button onClick={onStart} disabled={status?.running}>시작</button>
        <button onClick={onStop} disabled={!status?.running}>정지</button>
        <button onClick={onReset}>리셋</button>
      </section>

      <section>
        <div style={{ background: "#eee", borderRadius: 8, overflow: "hidden", height: 24 }}>
          <div style={{ width: `${pct}%`, height: "100%", background: "#4f46e5" }} />
        </div>
        <p>
          진행: <strong>{sent} / {total}</strong> ({pct}%) · 실패 {status?.errors ?? 0} ·
          {" "}경과 {status?.elapsedSeconds ?? 0}s · 남음 {status?.etaSeconds ?? 0}s ·
          {" "}분당 {(status?.ratePerMin ?? 0).toFixed(1)} · LLM {status?.llmClient ?? "-"} ·
          {" "}{status?.running ? "실행중" : "정지"}
        </p>
      </section>
    </div>
  );
}
```

- [ ] **Step 5: main.tsx에 라우트 추가**

`frontend/src/main.tsx`의 import 블록에 추가:
```tsx
import { SimulationControlPage } from "./dev/SimulationControlPage";
```
`<Routes>` 안, 공개 라우트(`/submit`) 다음 줄에 추가:
```tsx
          {/* 개발용: 트래픽 시뮬레이터 제어판 (sim 프로파일 백엔드 필요) */}
          <Route path="/dev/sim" element={<SimulationControlPage />} />
```

- [ ] **Step 6: 테스트 통과 확인**

Run (frontend 디렉터리): `npm test -- --run src/dev/SimulationControlPage.test.tsx`
Expected: PASS (2건).

- [ ] **Step 7: 프론트 빌드 확인**

Run (frontend 디렉터리): `npm run build`
Expected: tsc + vite 빌드 성공 (타입 에러 없음).

- [ ] **Step 8: 커밋**

```bash
git add frontend/src/dev/simApi.ts \
        frontend/src/dev/SimulationControlPage.tsx \
        frontend/src/dev/SimulationControlPage.test.tsx \
        frontend/src/main.tsx
git commit -m "feat(fe-sim): 트래픽 시뮬레이터 제어판(/dev/sim)"
```

---

### Task 9: 수동 E2E 검증 (자동화 불가 영역)

agentcli 실호출 + 자기호출 HTTP + 드립은 단위테스트로 덮이지 않으므로 1회 수동 검증한다.

**Files:** 없음 (실행/관찰만)

- [ ] **Step 1: 빠른 리허설 (mock + 소량)** — 요금/지연 없이 배선 확인

```bash
# backend 디렉터리에서
LLM_CLIENT=mock ./gradlew bootRun --args='--spring.profiles.active=sim'
```
다른 터미널:
```bash
curl -s -X POST localhost:8080/api/v1/dev/simulation/start \
  -H 'Content-Type: application/json' -d '{"count":5,"durationMinutes":1}' | jq
sleep 20
curl -s localhost:8080/api/v1/dev/simulation/status | jq
curl -s localhost:8080/api/v1/dashboard/board -H 'X-Operator-Id: 00000000-0000-0000-0000-000000000000' | jq 'keys'
```
Expected: status.sent 증가 → 5 도달, 칸반 board에 카드 유입(RECEIVED→PENDING_ASSIGNMENT). 시드 로그 `accounts=100 payments=100 items=500` 확인.

- [ ] **Step 2: agentcli 소량 검증** — 실제 `claude -p` 1~2건

```bash
# application-sim.yml 기본이 agentcli이므로 LLM_CLIENT 미지정
./gradlew bootRun --args='--spring.profiles.active=sim'
```
```bash
curl -s -X POST localhost:8080/api/v1/dev/simulation/start \
  -H 'Content-Type: application/json' -d '{"count":2,"durationMinutes":1}' | jq
```
Expected: 백엔드 로그에 `claude` 프로세스 분석 진행, 분석 완료 시 `[ai-pipeline] 분석 완료` 로그. 진단/초안이 시나리오 데이터와 정합.

- [ ] **Step 3: 제어판 확인**

```bash
# frontend 디렉터리에서
npm run dev
```
브라우저 `http://localhost:5173/dev/sim` → 건수/소요 설정 후 "시작" → 진행바·sent/total 갱신 관찰. (제어판은 :8080 백엔드를 직접 호출하므로 sim 백엔드가 떠 있어야 함)

- [ ] **Step 4: 검증 결과 기록**

`docs/superpowers/plans/2026-06-21-cs-traffic-simulator.md` 하단 또는 커밋 메시지에 관찰 결과 1줄 기록. 이상 시 systematic-debugging으로 전환.

---

## Self-Review

**1. Spec coverage**
- 가상 계정 100 → Task 4 `SimDataSeeder`(accounts=count=100). ✓
- 아이템 500 + 계정 연동 → Task 4 패딩으로 items=itemTarget=500, userId 연결. ✓
- 구매(결제)이력 → Task 4 scenario별 payment. ✓
- 데이터 기반 100건 CS → Task 2·3 ScenarioAssigner+Catalog (정합 본문). ✓
- 20분 드립 + 실제 유입 → Task 6 SimulationService + Task 7 HttpInquirySender(REST). ✓
- agentcli LLM(기본) → Task 1 AgentCliClient + Task 5 yml `llm-client: agentcli`. ✓
- 프론트 제어판 → Task 8. ✓
- 안전장치(sim 격리) → 전 컴포넌트 `@Profile("sim")`/`@ConditionalOnProperty`. ✓
- 동시성 제한 → Task 5 yml 풀 3/4. ✓

**2. Placeholder scan** — TBD/TODO/“적절히 처리” 없음. 모든 step에 실제 코드/명령 포함.

**3. Type consistency**
- `SimAssignment{userId,scenario,index}` — Task 2 정의, Task 3·4에서 `.userId()/.scenario()/.index()` 일관 사용. ✓
- `PlannedInquiry{userId,type,content,scenario}` — Task 3 정의, Task 6·7·8(프론트 타입 동형) 일관. ✓
- `SimulationStatus` 9필드 — Task 6 정의, Task 7 위임, Task 8 프론트 인터페이스 동형. ✓
- `SimProperties` getter/setter — Task 4 스텁=최종, Task 5 테스트·Task 6 사용 일관. ✓
- `SimulationService.start(Integer,Integer,Boolean)` — Task 6 정의, Task 7 컨트롤러 호출 일관. ✓
- `InquirySender.send(PlannedInquiry)` — Task 6 정의, Task 7 구현 일관. ✓

이상 없음.
