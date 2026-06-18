package com.ggteam.cs.workflow.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 공유 DTO: InquiryDetail (01-api-contract §6).
 * 미완료 단계의 필드는 null로 반환된다.
 *
 * <p>상세 조립(US-23)은 백엔드 A 소유이나, C의 Pull 배정 응답에 동일 구조가 필요하므로
 * C 브랜치에서 계약 기준으로 정의한다. 머지 시 단일 정의로 통합한다.
 */
@JsonInclude(JsonInclude.Include.ALWAYS)
public record InquiryDetailDto(
        InquiryPart inquiry,
        AnalysisPart analysis,
        DiagnosisPart diagnosis,
        DraftPart currentDraft,
        List<HistoryEntry> history) {

    public record InquiryPart(
            UUID inquiryId,
            Map<String, String> customerInfo,
            String customerType,
            String content,
            String status,
            ZonedDateTime createdAt,
            UUID assignedOperator) {}

    public record AnalysisPart(
            String aiType,
            String subCategory,
            String urgency,
            String summary,
            List<String> keywords,
            Map<String, Object> systemQueryResult,
            String failureType) {}

    public record DiagnosisPart(
            String cause,
            String suggestedDirection,
            BigDecimal confidence) {}

    public record DraftPart(
            UUID draftId,
            String content,
            String status,
            int regenerationCount) {}
}
