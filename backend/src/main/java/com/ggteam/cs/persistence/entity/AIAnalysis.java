package com.ggteam.cs.persistence.entity;

import com.ggteam.cs.common.enums.FailureType;
import com.ggteam.cs.common.enums.InquiryType;
import com.ggteam.cs.common.enums.Urgency;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * AI 분석 결과. (domain-entities §2.2) 문의당 1건(inquiryId UNIQUE).
 *
 * <p><b>소유: 백엔드 B.</b> 백엔드 C 브랜치에서는 대시보드/상세 조회용으로 stub 선행 작성.
 */
@Entity
@Table(name = "ai_analysis")
public class AIAnalysis {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(name = "inquiry_id", nullable = false, unique = true)
    private UUID inquiryId;

    @Enumerated(EnumType.STRING)
    @Column(name = "ai_type", nullable = false)
    private InquiryType aiType;

    @Column(name = "sub_category")
    private String subCategory;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Urgency urgency;

    @Column(columnDefinition = "text")
    private String summary;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column
    private List<String> keywords;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "system_query_result")
    private Map<String, Object> systemQueryResult;

    @Column(name = "analyzed_at")
    private ZonedDateTime analyzedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "failure_type")
    private FailureType failureType;

    protected AIAnalysis() {}

    /** 로컬/시딩용 성공 분석 생성 팩토리. */
    public static AIAnalysis success(UUID inquiryId, InquiryType aiType, String subCategory,
                                     Urgency urgency, String summary, List<String> keywords,
                                     Map<String, Object> systemQueryResult) {
        AIAnalysis a = new AIAnalysis();
        a.id = UUID.randomUUID();
        a.inquiryId = inquiryId;
        a.aiType = aiType;
        a.subCategory = subCategory;
        a.urgency = urgency;
        a.summary = summary;
        a.keywords = keywords;
        a.systemQueryResult = systemQueryResult;
        a.analyzedAt = ZonedDateTime.now();
        a.failureType = null;
        return a;
    }

    public UUID getId() {
        return id;
    }

    public UUID getInquiryId() {
        return inquiryId;
    }

    public InquiryType getAiType() {
        return aiType;
    }

    public String getSubCategory() {
        return subCategory;
    }

    public Urgency getUrgency() {
        return urgency;
    }

    public String getSummary() {
        return summary;
    }

    public List<String> getKeywords() {
        return keywords;
    }

    public Map<String, Object> getSystemQueryResult() {
        return systemQueryResult;
    }

    public ZonedDateTime getAnalyzedAt() {
        return analyzedAt;
    }

    public FailureType getFailureType() {
        return failureType;
    }
}
