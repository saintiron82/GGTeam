package com.ggteam.cs.aipipeline.model;

import com.ggteam.cs.common.enums.InquiryType;
import com.ggteam.cs.common.enums.Urgency;

import java.util.List;

/**
 * AI 분류/요약 결과 값 객체 (엔티티 아님, US-06).
 * AIAnalysisService가 이 값을 AIAnalysis 엔티티로 영속화한다 (백엔드 A 엔티티 결합 시).
 */
public record AnalysisResult(
        InquiryType aiType,
        String subCategory,
        Urgency urgency,
        String summary,
        List<String> keywords
) {}
