package com.ggteam.cs.external;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * BedrockClient 실제 연동 테스트.
 * AWS 자격증명(환경변수)이 있을 때만 실행된다 (CI/로컬 자격증명 없으면 skip).
 */
class BedrockClientIT {

    @Test
    @EnabledIfEnvironmentVariable(named = "AWS_ACCESS_KEY_ID", matches = ".+")
    void 실제_Bedrock_호출_성공() {
        String region = System.getenv().getOrDefault("AWS_REGION", "us-east-1");
        String modelId = System.getenv().getOrDefault("BEDROCK_MODEL_ID", "us.anthropic.claude-sonnet-4-6");

        BedrockClient client = new BedrockClient(region, modelId, 120, 3);
        LlmClient.LlmResponse res = client.complete(
                LlmClient.LlmRequest.of("[TEST] '연동성공'이라는 단어만 정확히 출력해줘."));

        assertNotNull(res);
        assertNotNull(res.content());
        assertFalse(res.content().isBlank(), "응답이 비어있지 않아야 함");
        System.out.println("[BedrockClientIT] 모델 응답: " + res.content());
    }
}
