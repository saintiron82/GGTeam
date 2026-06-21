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
