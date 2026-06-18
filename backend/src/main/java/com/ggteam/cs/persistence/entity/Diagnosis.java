package com.ggteam.cs.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.UUID;

/**
 * 진단(원인/처리방향/신뢰도). (domain-entities §2.3) 문의당 1건(inquiryId UNIQUE).
 *
 * <p><b>소유: 백엔드 B.</b> 백엔드 C 브랜치에서는 상세 조회용으로 stub 선행 작성.
 */
@Entity
@Table(name = "diagnosis")
public class Diagnosis {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(name = "inquiry_id", nullable = false, unique = true)
    private UUID inquiryId;

    @Column(nullable = false, columnDefinition = "text")
    private String cause;

    @Column(name = "suggested_direction", nullable = false, columnDefinition = "text")
    private String suggestedDirection;

    /** 0.0 ~ 1.0 (BR-05). */
    @Column(nullable = false)
    private BigDecimal confidence;

    protected Diagnosis() {}

    /** 로컬/시딩용 진단 생성 팩토리. */
    public static Diagnosis of(UUID inquiryId, String cause, String suggestedDirection, BigDecimal confidence) {
        Diagnosis d = new Diagnosis();
        d.id = UUID.randomUUID();
        d.inquiryId = inquiryId;
        d.cause = cause;
        d.suggestedDirection = suggestedDirection;
        d.confidence = confidence;
        return d;
    }

    public UUID getId() {
        return id;
    }

    public UUID getInquiryId() {
        return inquiryId;
    }

    public String getCause() {
        return cause;
    }

    public String getSuggestedDirection() {
        return suggestedDirection;
    }

    public BigDecimal getConfidence() {
        return confidence;
    }
}
