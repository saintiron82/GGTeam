package com.ggteam.cs.inquiry.dto;

import com.ggteam.cs.auth.dto.OperatorSummary;
import com.ggteam.cs.common.enums.ApprovalAction;
import com.ggteam.cs.common.enums.DraftResponseStatus;
import com.ggteam.cs.common.enums.FailureType;
import com.ggteam.cs.common.enums.InquiryStatus;
import com.ggteam.cs.common.enums.InquiryType;
import com.ggteam.cs.common.enums.Urgency;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 문의 상세 조회 조립 DTO (01-api-contract §6, US-23).
 *
 * <p>여러 도메인(문의/분석/진단/초안/이력)을 한 응답으로 조립한다. 미완료 단계의 섹션은
 * {@code null}로 반환한다(예: 분석 전이면 analysis=null).
 *
 * <p>담당: 백엔드 A (조립). 분석/진단/초안 데이터 생성: 백엔드 B, 이력: 백엔드 C.
 */
public record InquiryDetail(
        InquirySection inquiry,
        AnalysisSection analysis,
        DiagnosisSection diagnosis,
        DraftSection currentDraft,
        List<HistoryItem> history
) {

    /** 문의 기본 정보 섹션. */
    public record InquirySection(
            UUID inquiryId,
            Map<String, Object> customerInfo,
            InquiryType customerType,
            String content,
            InquiryStatus status,
            ZonedDateTime createdAt,
            OperatorSummary assignedOperator
    ) {}

    /** AI 분석 섹션 (분석 전이면 null). */
    public record AnalysisSection(
            InquiryType aiType,
            String subCategory,
            Urgency urgency,
            String summary,
            List<String> keywords,
            Map<String, Object> systemQueryResult,
            FailureType failureType
    ) {}

    /** 진단 섹션 (진단 전이면 null). */
    public record DiagnosisSection(
            String cause,
            String suggestedDirection,
            double confidence
    ) {}

    /** 활성(current) 초안 섹션 (초안 전이면 null). */
    public record DraftSection(
            UUID draftId,
            String content,
            DraftResponseStatus status,
            int regenerationCount
    ) {}

    /** 처리 이력 항목. */
    public record HistoryItem(
            ApprovalAction action,
            OperatorSummary operator,
            String reason,
            ZonedDateTime timestamp
    ) {}
}
