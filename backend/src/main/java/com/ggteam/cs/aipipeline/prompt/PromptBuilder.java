package com.ggteam.cs.aipipeline.prompt;

import com.ggteam.cs.external.LlmClient;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * LLM 프롬프트 빌더. 외부 JSON 템플릿(PromptLoader)을 로드하여 플레이스홀더를 치환하고
 * system/prompt/maxTokens를 담은 {@link LlmClient.LlmRequest}를 생성한다.
 *
 * <p>프롬프트 내용 자체는 resources/prompts/*.json 에 있어 재빌드 없이 편집 가능.
 * 담당: 백엔드 B (US-06, US-11, US-13, US-17).
 */
@Component
public class PromptBuilder {

    private final PromptLoader loader;

    public PromptBuilder(PromptLoader loader) {
        this.loader = loader;
    }

    /** 분류/요약 요청 (US-06). */
    public LlmClient.LlmRequest classify(String inquiryContent) {
        PromptTemplate t = loader.load("classify");
        String prompt = render(t.userTemplate(), Map.of("content", safe(inquiryContent)));
        return LlmClient.LlmRequest.of(t.system(), prompt, t.maxTokens());
    }

    /** 원인 진단 요청 (US-11). */
    public LlmClient.LlmRequest diagnose(String summary, Map<String, Object> systemData) {
        PromptTemplate t = loader.load("diagnose");
        String prompt = render(t.userTemplate(), Map.of(
                "summary", safe(summary),
                "systemData", String.valueOf(systemData)));
        return LlmClient.LlmRequest.of(t.system(), prompt, t.maxTokens());
    }

    /** 답변 초안 요청 (US-13). rejectReason이 있으면 재생성 컨텍스트 반영 (US-17). */
    public LlmClient.LlmRequest draft(String cause, String suggestedDirection, String rejectReason) {
        PromptTemplate t = loader.load("draft");
        String rejectionContext = (rejectReason == null || rejectReason.isBlank())
                ? ""
                : "\n\n이전 답변은 다음 사유로 반려되었다. 이를 반영하여 개선하라: " + rejectReason;
        String prompt = render(t.userTemplate(), Map.of(
                "cause", safe(cause),
                "suggestedDirection", safe(suggestedDirection),
                "rejectionContext", rejectionContext));
        return LlmClient.LlmRequest.of(t.system(), prompt, t.maxTokens());
    }

    /** {key} 플레이스홀더를 값으로 치환. */
    private String render(String template, Map<String, String> vars) {
        String result = template;
        for (Map.Entry<String, String> e : vars.entrySet()) {
            result = result.replace("{" + e.getKey() + "}", e.getValue());
        }
        return result;
    }

    private String safe(String s) {
        return s == null ? "" : s;
    }
}
