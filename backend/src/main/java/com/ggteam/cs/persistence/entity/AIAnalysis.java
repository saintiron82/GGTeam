package com.ggteam.cs.persistence.entity;

import com.ggteam.cs.common.enums.FailureType;
import com.ggteam.cs.common.enums.InquiryType;
import com.ggteam.cs.common.enums.Urgency;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * AI 분석 결과(분류·요약·시스템 조회 통합). 문의당 최대 1건 (BR-39, inquiryId UNIQUE).
 * (domain-entities.md §2.2)
 *
 * <p>정상 완료 시 failureType=null, analyzedAt 기록 (BR-29).
 * 실패 시 failureType 기록 후 MANUAL_CLASSIFICATION_PENDING 전이 (BR-28).
 *
 * <p>엔티티/스키마 소유: 백엔드 A. 비즈니스 로직: 백엔드 B.
 */
@Entity
@Table(name = "ai_analysis")
@Getter
@Setter
@NoArgsConstructor
public class AIAnalysis extends BaseEntity {

    /** 대상 문의 (1:1, UNIQUE). FK→inquiry.id. */
    @Column(name = "inquiry_id", nullable = false, unique = true)
    private UUID inquiryId;

    /** AI가 분류한 문의 유형. */
    @Enumerated(EnumType.STRING)
    @Column(name = "ai_type", nullable = false, length = 20)
    private InquiryType aiType;

    /** 세부 분류 (예: 결제실패, 중복결제, 환불요청). */
    @Column(name = "sub_category", length = 100)
    private String subCategory;

    @Enumerated(EnumType.STRING)
    @Column(name = "urgency", nullable = false, length = 20)
    private Urgency urgency;

    @Column(name = "summary", columnDefinition = "text")
    private String summary;

    /** 추출 키워드 배열. jsonb. */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "keywords", columnDefinition = "jsonb")
    private List<String> keywords;

    /** SystemDataQueryService 조회 결과 스냅샷. jsonb. */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "system_query_result", columnDefinition = "jsonb")
    private Map<String, Object> systemQueryResult;

    /** 분석 완료 시각. 실패 시 시도 종료 시각. */
    @Column(name = "analyzed_at")
    private ZonedDateTime analyzedAt;

    /** 실패 유형. 정상 시 null (BR-29). */
    @Enumerated(EnumType.STRING)
    @Column(name = "failure_type", length = 20)
    private FailureType failureType;

    /** 정상 분석 결과 팩토리 (failureType=null, analyzedAt=now, BR-29). */
    public static AIAnalysis success(UUID inquiryId, InquiryType aiType, String subCategory,
                                     Urgency urgency, String summary, List<String> keywords,
                                     Map<String, Object> systemQueryResult) {
        AIAnalysis a = new AIAnalysis();
        a.setInquiryId(inquiryId);
        a.setAiType(aiType);
        a.setSubCategory(subCategory);
        a.setUrgency(urgency);
        a.setSummary(summary);
        a.setKeywords(keywords);
        a.setSystemQueryResult(systemQueryResult);
        a.setAnalyzedAt(ZonedDateTime.now());
        return a;
    }

    /** 분석 실패 결과 팩토리 (failureType 기록, BR-28). */
    public static AIAnalysis failed(UUID inquiryId, FailureType failureType) {
        AIAnalysis a = new AIAnalysis();
        a.setInquiryId(inquiryId);
        a.setFailureType(failureType);
        a.setAnalyzedAt(ZonedDateTime.now());
        return a;
    }
}
