package com.ggteam.cs.aipipeline;

import com.ggteam.cs.aipipeline.model.DiagnosisResult;
import com.ggteam.cs.aipipeline.parse.LlmResponseParser;
import com.ggteam.cs.aipipeline.prompt.PromptBuilder;
import com.ggteam.cs.external.LlmClient;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * AI 원인 진단 (US-11). 백엔드 B.
 * 문의 요약 + 시스템 조회 결과를 근거로 LLM 진단을 수행한다.
 */
@Service
public class DiagnosisService {

    private final LlmClient llmClient;
    private final PromptBuilder promptBuilder;
    private final LlmResponseParser parser;

    public DiagnosisService(LlmClient llmClient, PromptBuilder promptBuilder, LlmResponseParser parser) {
        this.llmClient = llmClient;
        this.promptBuilder = promptBuilder;
        this.parser = parser;
    }

    public DiagnosisResult diagnose(String summary, Map<String, Object> systemData) {
        LlmClient.LlmResponse res = llmClient.complete(promptBuilder.diagnose(summary, systemData));
        return parser.parseDiagnosis(res.content());
    }
}
