package com.ggteam.cs.persistence.entity;

import com.ggteam.cs.common.enums.DraftResponseStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.ZonedDateTime;
import java.util.UUID;

/**
 * 답변 초안. 재생성 시 새 레코드로 관리 (1:N). 동일 inquiryId 내 최신 1건이 활성(current) 초안.
 * (domain-entities.md §2.4)
 *
 * <p>regenerationCount는 음수 불가, 단조 증가 (BR-42), 최대 3회 (BR-19).
 *
 * <p>엔티티/스키마 소유: 백엔드 A. 비즈니스 로직: 백엔드 B.
 */
@Entity
@Table(name = "draft_response")
@Getter
@Setter
@NoArgsConstructor
public class DraftResponse extends BaseEntity {

    /** 대상 문의 (N:1). FK→inquiry.id. */
    @Column(name = "inquiry_id", nullable = false)
    private UUID inquiryId;

    /** 답변 초안 본문. */
    @Column(name = "content", nullable = false, columnDefinition = "text")
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private DraftResponseStatus status = DraftResponseStatus.GENERATED;

    /** 재생성 누적 횟수. 기본 0, 단조 증가 (BR-18, BR-42). */
    @Column(name = "regeneration_count", nullable = false)
    private int regenerationCount = 0;

    /** 생성 시각 (KST, 자동 생성). */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private ZonedDateTime createdAt;
}
