package com.ggteam.cs.external;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.awscore.AwsRequestOverrideConfiguration;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.core.exception.ApiCallAttemptTimeoutException;
import software.amazon.awssdk.core.exception.ApiCallTimeoutException;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient;
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelRequest;
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelResponse;

import java.time.Duration;

/**
 * AWS Bedrock 기반 LlmClient 구현 (NFR-03, US-06/08).
 *
 * <p>정책:
 * <ul>
 *   <li>타임아웃: 시도마다 점증 (base, base*1.5, base*2 = 120/180/240s) + exponential backoff (BR-26, BR-26a)</li>
 *   <li>타임아웃 재시도 최대 maxRetries회 소진 시 {@link LlmTimeoutException}</li>
 *   <li>그 외 API 오류는 재시도 없이 {@link LlmApiException} (BR-27)</li>
 * </ul>
 *
 * <p>활성화: app.ai.llm-client=bedrock
 */
@Component
@ConditionalOnProperty(name = "app.ai.llm-client", havingValue = "bedrock", matchIfMissing = true)
public class BedrockClient implements LlmClient {

    private static final Logger log = LoggerFactory.getLogger(BedrockClient.class);
    private static final String ANTHROPIC_VERSION = "bedrock-2023-05-31";

    private final BedrockRuntimeClient client;
    private final ObjectMapper mapper = new ObjectMapper();
    private final String modelId;
    private final int baseTimeoutSeconds;
    private final int maxRetries;

    public BedrockClient(
            @Value("${app.bedrock.region:us-east-1}") String region,
            @Value("${app.bedrock.model-id}") String modelId,
            @Value("${app.bedrock.timeout-seconds:120}") int baseTimeoutSeconds,
            @Value("${app.bedrock.max-retries:3}") int maxRetries) {
        this.modelId = modelId;
        this.baseTimeoutSeconds = baseTimeoutSeconds;
        this.maxRetries = maxRetries;
        // 자격증명은 DefaultCredentialsProvider 체인(환경변수/프로파일/역할)에서 자동 해결
        this.client = BedrockRuntimeClient.builder()
                .region(Region.of(region))
                .build();
    }

    @Override
    public LlmResponse complete(LlmRequest request) {
        String body = buildAnthropicBody(request);

        int attempt = 0;
        while (true) {
            int timeoutSeconds = timeoutForAttempt(attempt);
            try {
                InvokeModelRequest req = InvokeModelRequest.builder()
                        .modelId(modelId)
                        .contentType("application/json")
                        .accept("application/json")
                        .body(SdkBytes.fromUtf8String(body))
                        .overrideConfiguration(AwsRequestOverrideConfiguration.builder()
                                .apiCallTimeout(Duration.ofSeconds(timeoutSeconds))
                                .build())
                        .build();

                InvokeModelResponse res = client.invokeModel(req);
                return new LlmResponse(extractText(res.body().asUtf8String()));

            } catch (ApiCallTimeoutException | ApiCallAttemptTimeoutException e) {
                attempt++;
                if (attempt > maxRetries) {
                    log.warn("Bedrock 타임아웃 재시도 소진 ({}회). modelId={}", maxRetries, modelId);
                    throw new LlmTimeoutException("Bedrock 호출 타임아웃 (재시도 " + maxRetries + "회 소진)", e);
                }
                backoff(attempt);
                log.info("Bedrock 타임아웃, 재시도 {}/{} (다음 timeout={}s)", attempt, maxRetries, timeoutForAttempt(attempt));
            } catch (LlmApiException e) {
                throw e;
            } catch (Exception e) {
                // 인증/요청/모델 오류 등 → 즉시 실패 (재시도 안 함)
                throw new LlmApiException("Bedrock API 오류: " + e.getMessage(), e);
            }
        }
    }

    /** 시도별 타임아웃 점증: 120 → 180 → 240 ... (base * (1 + 0.5*attempt)). */
    private int timeoutForAttempt(int attempt) {
        return (int) Math.round(baseTimeoutSeconds * (1.0 + 0.5 * attempt));
    }

    /** exponential backoff: 1s, 2s, 4s ... */
    private void backoff(int attempt) {
        try {
            Thread.sleep((long) Math.pow(2, attempt - 1) * 1000L);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            throw new LlmApiException("재시도 대기 중 인터럽트", ie);
        }
    }

    private String buildAnthropicBody(LlmRequest request) {
        try {
            ObjectNode root = mapper.createObjectNode();
            root.put("anthropic_version", ANTHROPIC_VERSION);
            root.put("max_tokens", request.maxTokens());
            ArrayNode messages = root.putArray("messages");
            ObjectNode userMsg = messages.addObject();
            userMsg.put("role", "user");
            userMsg.put("content", request.prompt());
            return mapper.writeValueAsString(root);
        } catch (Exception e) {
            throw new LlmApiException("요청 본문 직렬화 실패", e);
        }
    }

    /** Anthropic messages 응답에서 첫 텍스트 블록 추출. */
    private String extractText(String responseJson) {
        try {
            JsonNode root = mapper.readTree(responseJson);
            JsonNode content = root.path("content");
            if (content.isArray() && !content.isEmpty()) {
                return content.get(0).path("text").asText("");
            }
            return "";
        } catch (Exception e) {
            throw new LlmApiException("응답 파싱 실패", e);
        }
    }
}
