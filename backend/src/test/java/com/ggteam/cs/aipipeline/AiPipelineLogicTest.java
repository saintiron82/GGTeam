package com.ggteam.cs.aipipeline;

import com.ggteam.cs.aipipeline.model.AnalysisResult;
import com.ggteam.cs.aipipeline.model.DiagnosisResult;
import com.ggteam.cs.aipipeline.parse.LlmResponseParser;
import com.ggteam.cs.aipipeline.prompt.PromptBuilder;
import com.ggteam.cs.aipipeline.quality.ResponseQualityValidator;
import com.ggteam.cs.common.enums.InquiryType;
import com.ggteam.cs.common.enums.Urgency;
import com.ggteam.cs.external.LlmClient;
import com.ggteam.cs.external.MockLlmClient;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 백엔드 B 순수 로직 단위 테스트 (엔티티/AWS 의존 없음).
 * MockLlmClient + PromptBuilder + Parser + QualityValidator 연동 검증.
 */
class AiPipelineLogicTest {

    private final PromptBuilder promptBuilder = new PromptBuilder();
    private final LlmResponseParser parser = new LlmResponseParser();
    private final ResponseQualityValidator validator = new ResponseQualityValidator();
    private final LlmClient mockLlm = new MockLlmClient();

    @Test
    void 분류_프롬프트_생성_및_Mock응답_파싱() {
        // US-06: 프롬프트 생성 → Mock LLM → 파싱
        String prompt = promptBuilder.classify("결제했는데 아이템이 안 들어왔어요");
        assertTrue(prompt.contains("[CLASSIFY]"));

        LlmClient.LlmResponse res = mockLlm.complete(LlmClient.LlmRequest.of(prompt));
        AnalysisResult result = parser.parseAnalysis(res.content());

        assertEquals(InquiryType.PAYMENT, result.aiType());
        assertEquals(Urgency.HIGH, result.urgency());
        assertNotNull(result.summary());
        assertFalse(result.keywords().isEmpty());
    }

    @Test
    void 진단_프롬프트_생성_및_Mock응답_파싱() {
        // US-11
        String prompt = promptBuilder.diagnose("아이템 미지급", Map.of("payment", "SUCCESS", "delivery", "NOT_DELIVERED"));
        assertTrue(prompt.contains("[DIAGNOSE]"));

        LlmClient.LlmResponse res = mockLlm.complete(LlmClient.LlmRequest.of(prompt));
        DiagnosisResult result = parser.parseDiagnosis(res.content());

        assertNotNull(result.cause());
        assertNotNull(result.suggestedDirection());
        assertTrue(result.confidence() >= 0.0 && result.confidence() <= 1.0);
    }

    @Test
    void 초안_프롬프트_생성_및_품질검증_통과() {
        // US-13, BR-30d
        String prompt = promptBuilder.draft("시스템 오류로 지급 누락", "재지급 안내", null);
        assertTrue(prompt.contains("[DRAFT]"));

        LlmClient.LlmResponse res = mockLlm.complete(LlmClient.LlmRequest.of(prompt));
        ResponseQualityValidator.QualityResult quality = validator.validate(res.content());

        assertTrue(quality.valid(), "정상 초안은 품질 검증 통과해야 함");
    }

    @Test
    void 반려사유_재생성_프롬프트에_반영() {
        // US-17: 반려 사유가 프롬프트에 포함되어야 함
        String prompt = promptBuilder.draft("원인", "방향", "너무 형식적임");
        assertTrue(prompt.contains("너무 형식적임"));
        assertTrue(prompt.contains("반려"));
    }

    @Test
    void 품질검증_불량_케이스() {
        // BR-30d: 빈 응답, 길이 미달
        assertFalse(validator.validate("").valid());
        assertFalse(validator.validate("   ").valid());
        assertFalse(validator.validate("짧음").valid());
        assertTrue(validator.validate("이것은 충분히 긴 정상적인 답변 문장입니다.").valid());
    }

    @Test
    void 파서_JSON앞뒤_설명문_혼입_처리() {
        // LLM이 JSON 앞뒤에 설명을 붙여도 추출되어야 함
        String messy = "다음은 결과입니다:\n{\"cause\":\"원인\",\"suggestedDirection\":\"방향\",\"confidence\":1.5}\n참고하세요.";
        DiagnosisResult r = parser.parseDiagnosis(messy);
        assertEquals("원인", r.cause());
        assertEquals(1.0, r.confidence()); // 1.5 → 1.0 클램프
    }
}
