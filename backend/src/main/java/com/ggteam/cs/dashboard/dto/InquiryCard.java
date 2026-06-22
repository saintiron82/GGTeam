package com.ggteam.cs.dashboard.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.ZonedDateTime;
import java.util.UUID;

/**
 * 대시보드/칸반 카드 (01-api-contract §4).
 * Inquiry + AIAnalysis 조립 결과. 분석 전이면 aiType/urgency/summary는 null.
 */
@JsonInclude(JsonInclude.Include.ALWAYS)
public record InquiryCard(
        UUID inquiryId,
        String customerType,
        String aiType,
        String urgency,
        String summary,
        String content,
        String status,
        UUID assignedOperator,
        ZonedDateTime createdAt) {}
