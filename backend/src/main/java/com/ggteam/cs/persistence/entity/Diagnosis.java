package com.ggteam.cs.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

/**
 * 진단(원인 분석 + 처리 방향 제안). 문의당 최대 1건 (BR-39, inquiryId UNIQUE).
 * (domain-entities.md §2.3)
 *
 * <p>confidence는 0.0~1.0 (BR-05).
 *
 * <p>엔티티/스키마 소유: 백엔드 A. 비즈니스 로직: 백엔드 B.
 */
@Entity
@Table(name = "diagnosis")
@Getter
@Setter
@NoArgsConstructor
public class Diagnosis extends BaseEntity {

    /** 대상 문의 (1:1, UNIQUE). FK→inquiry.id. */
    @Column(name = "inquiry_id", nullable = false, unique = true)
    private UUID inquiryId;

    /** 추정 원인. */
    @Column(name = "cause", nullable = false, columnDefinition = "text")
    private String cause;

    /** 제안 처리 방향. */
    @Column(name = "suggested_direction", nullable = false, columnDefinition = "text")
    private String suggestedDirection;

    /** 진단 신뢰도 0.0~1.0 (BR-05). */
    @Column(name = "confidence", nullable = false)
    private double confidence;

    /** 진단 생성 팩토리 (id 자동). */
    public static Diagnosis of(UUID inquiryId, String cause, String suggestedDirection, double confidence) {
        Diagnosis d = new Diagnosis();
        d.setInquiryId(inquiryId);
        d.setCause(cause);
        d.setSuggestedDirection(suggestedDirection);
        d.setConfidence(confidence);
        return d;
    }
}
