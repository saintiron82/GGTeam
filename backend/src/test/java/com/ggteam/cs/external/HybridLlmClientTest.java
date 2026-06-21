package com.ggteam.cs.external;

import static org.assertj.core.api.Assertions.assertThat;

import com.ggteam.cs.external.LlmClient.LlmRequest;
import com.ggteam.cs.external.LlmClient.LlmResponse;
import org.junit.jupiter.api.Test;

class HybridLlmClientTest {

    private static LlmClient fixed(String content) {
        return request -> new LlmResponse(content);
    }

    @Test
    void 예산_이내는_agentcli_이후는_mock으로_라우팅() {
        HybridLlmClient client = new HybridLlmClient(fixed("AGENT"), fixed("MOCK"), 2);

        assertThat(client.complete(LlmRequest.of("1")).content()).isEqualTo("AGENT");
        assertThat(client.complete(LlmRequest.of("2")).content()).isEqualTo("AGENT");
        assertThat(client.complete(LlmRequest.of("3")).content()).isEqualTo("MOCK");
        assertThat(client.complete(LlmRequest.of("4")).content()).isEqualTo("MOCK");
    }

    @Test
    void agentcli_실패시_해당_호출은_mock으로_폴백() {
        LlmClient failing = request -> { throw new LlmApiException("boom", null); };
        HybridLlmClient client = new HybridLlmClient(failing, fixed("MOCK"), 5);

        assertThat(client.complete(LlmRequest.of("1")).content()).isEqualTo("MOCK");
    }

    @Test
    void 예산_0이면_전부_mock() {
        HybridLlmClient client = new HybridLlmClient(fixed("AGENT"), fixed("MOCK"), 0);

        assertThat(client.complete(LlmRequest.of("1")).content()).isEqualTo("MOCK");
    }
}
