package com.ggteam.cs.aipipeline.parse;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ggteam.cs.aipipeline.model.AnalysisResult;
import com.ggteam.cs.aipipeline.model.DiagnosisResult;
import com.ggteam.cs.common.enums.InquiryType;
import com.ggteam.cs.common.enums.Urgency;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * LLM JSON 응답을 도메인 값 객체로 파싱 (순수 로직, 엔티티 무관).
 * LLM이 JSON 앞뒤에 설명을 붙이는 경우를 대비해 첫 '{' ~ 마지막 '}' 구간을 추출한다.
 *
 * <p>담당: 백엔드 B. 파싱 실패 시 ParseException.
 */
@Component
public class LlmResponseParser {

    private final ObjectMapper mapper = new ObjectMapper();

    public AnalysisResult parseAnalysis(String llmContent) {
        JsonNode node = readJson(llmContent);
        List<String> keywords = new ArrayList<>();
        JsonNode kw = node.path("keywords");
        if (kw.isArray()) {
            kw.forEach(k -> keywords.add(k.asText()));
        }
        return new AnalysisResult(
                parseEnum(InquiryType.class, node.path("aiType").asText(null), InquiryType.ETC),
                node.path("subCategory").asText(null),
                parseEnum(Urgency.class, node.path("urgency").asText(null), Urgency.NORMAL),
                node.path("summary").asText(null),
                keywords
        );
    }

    public DiagnosisResult parseDiagnosis(String llmContent) {
        JsonNode node = readJson(llmContent);
        double confidence = node.path("confidence").asDouble(0.0);
        confidence = Math.max(0.0, Math.min(1.0, confidence)); // 0~1 클램프 (BR-05)
        return new DiagnosisResult(
                node.path("cause").asText(null),
                node.path("suggestedDirection").asText(null),
                confidence
        );
    }

    private JsonNode readJson(String content) {
        try {
            String json = extractJsonBlock(content);
            return mapper.readTree(json);
        } catch (Exception e) {
            throw new ParseException("LLM 응답 JSON 파싱 실패: " + e.getMessage(), e);
        }
    }

    /** 응답에서 첫 '{' ~ 마지막 '}' 구간 추출 (설명 텍스트 혼입 대비). */
    private String extractJsonBlock(String content) {
        if (content == null) {
            throw new ParseException("LLM 응답이 null", null);
        }
        int start = content.indexOf('{');
        int end = content.lastIndexOf('}');
        if (start < 0 || end < 0 || end < start) {
            throw new ParseException("JSON 블록을 찾을 수 없음: " + content, null);
        }
        return content.substring(start, end + 1);
    }

    private <E extends Enum<E>> E parseEnum(Class<E> type, String value, E fallback) {
        if (value == null) return fallback;
        try {
            return Enum.valueOf(type, value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return fallback;
        }
    }

    /** 파싱 실패 예외. */
    public static class ParseException extends RuntimeException {
        public ParseException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
