package com.ggteam.cs.external;

import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * 하이브리드 LlmClient (데모/시연용). 처음 N회 호출은 실제 agentcli(`claude -p`)로 품질을 보여주고,
 * 그 이후는 MockLlmClient로 빠르게 처리한다. 대량 시연에서 "샘플은 진짜 AI, 나머지는 즉시"를 만족한다.
 *
 * <p>활성화: app.ai.llm-client=hybrid. agentcli 호출 예산: app.ai.hybrid.agentcli-calls (기본 15 ≈ 5문의×3호출).
 * agentcli 호출이 실패하면 해당 호출은 mock으로 폴백한다(시연 흐름 보호).
 */
@Component
@ConditionalOnProperty(name = "app.ai.llm-client", havingValue = "hybrid")
public class HybridLlmClient implements LlmClient {

    private static final Logger log = LoggerFactory.getLogger(HybridLlmClient.class);

    private final LlmClient agentcli;
    private final LlmClient mock;
    private final int agentcliCalls;
    private final AtomicInteger used = new AtomicInteger();

    @Autowired
    public HybridLlmClient(
            @Value("${app.ai.agentcli.command:claude -p}") String command,
            @Value("${app.ai.agentcli.system-flag:--append-system-prompt}") String systemFlag,
            @Value("${app.ai.agentcli.timeout-seconds:120}") int timeoutSeconds,
            @Value("${app.ai.hybrid.agentcli-calls:15}") int agentcliCalls) {
        this(new AgentCliClient(command, systemFlag, timeoutSeconds), new MockLlmClient(), agentcliCalls);
    }

    /** 테스트/조립용. 두 위임 클라이언트와 예산을 직접 주입. */
    HybridLlmClient(LlmClient agentcli, LlmClient mock, int agentcliCalls) {
        this.agentcli = agentcli;
        this.mock = mock;
        this.agentcliCalls = agentcliCalls;
    }

    @Override
    public LlmResponse complete(LlmRequest request) {
        int n = used.getAndIncrement();
        if (n < agentcliCalls) {
            try {
                return agentcli.complete(request);
            } catch (Exception e) {
                log.warn("[hybrid] agentcli 호출 {} 실패 → mock 폴백: {}", n + 1, e.getMessage());
                return mock.complete(request);
            }
        }
        return mock.complete(request);
    }
}
