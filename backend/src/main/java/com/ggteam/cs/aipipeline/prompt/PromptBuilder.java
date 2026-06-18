package com.ggteam.cs.aipipeline.prompt;

import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * LLM 프롬프트 빌더 (순수 로직, 엔티티 무관).
 * 각 단계 마커([CLASSIFY]/[DIAGNOSE]/[DRAFT])를 포함하여 MockLlmClient와도 호환된다.
 *
 * <p>담당: 백엔드 B (US-06, US-11, US-13).
 */
@Component
public class PromptBuilder {

    /** 분류/요약 프롬프트 (US-06). */
    public String classify(String inquiryContent) {
        return """
            [CLASSIFY]
            너는 게임 CS 문의 분류기다. 아래 고객 문의를 분석하여 JSON으로만 답하라.
            형식: {"aiType":"PAYMENT|ITEM_DELIVERY|ACCOUNT|ETC","subCategory":"문자열","urgency":"HIGH|NORMAL|LOW","summary":"요약","keywords":["키워드"]}

            고객 문의:
            %s
            """.formatted(safe(inquiryContent));
    }

    /** 원인 진단 프롬프트 (US-11). systemData는 조회 결과 요약. */
    public String diagnose(String summary, Map<String, Object> systemData) {
        return """
            [DIAGNOSE]
            너는 게임 CS 진단 전문가다. 문의 요약과 시스템 조회 결과를 바탕으로 원인을 진단하라.
            JSON으로만 답하라.
            형식: {"cause":"원인","suggestedDirection":"처리 방향","confidence":0.0~1.0}

            문의 요약: %s
            시스템 조회 결과: %s
            """.formatted(safe(summary), String.valueOf(systemData));
    }

    /** 답변 초안 생성 프롬프트 (US-13). rejectReason이 있으면 재생성 컨텍스트 반영 (US-17). */
    public String draft(String cause, String suggestedDirection, String rejectReason) {
        String rejectionContext = (rejectReason == null || rejectReason.isBlank())
                ? ""
                : "\n이전 답변은 다음 사유로 반려되었다. 이를 반영하여 개선하라: " + rejectReason;
        return """
            [DRAFT]
            너는 게임 CS 운영자를 돕는 답변 작성기다. 아래 진단을 바탕으로 고객에게 보낼
            정중하고 명확한 한국어 답변 초안을 작성하라. 답변 본문만 출력하라.

            원인: %s
            처리 방향: %s%s
            """.formatted(safe(cause), safe(suggestedDirection), rejectionContext);
    }

    private String safe(String s) {
        return s == null ? "" : s;
    }
}
