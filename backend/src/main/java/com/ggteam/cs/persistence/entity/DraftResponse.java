package com.ggteam.cs.persistence.entity;

import com.ggteam.cs.common.enums.DraftResponseStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.ZonedDateTime;
import java.util.UUID;

/**
 * 답변 초안. (domain-entities §2.4) 재생성 시 새 레코드. 문의당 1:N.
 *
 * <p><b>소유: 백엔드 B.</b> 백엔드 C 브랜치에서는 승인/수정/반려 워크플로우용으로 stub 선행 작성.
 */
@Entity
@Table(name = "draft_response")
public class DraftResponse {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(name = "inquiry_id", nullable = false)
    private UUID inquiryId;

    @Column(nullable = false, columnDefinition = "text")
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DraftResponseStatus status = DraftResponseStatus.GENERATED;

    /** 재생성 누적 횟수 (BR-18, BR-42). 단조 증가, ≥0. */
    @Column(name = "regeneration_count", nullable = false)
    private int regenerationCount = 0;

    @Column(name = "created_at", nullable = false, updatable = false)
    private ZonedDateTime createdAt;

    protected DraftResponse() {}

    public DraftResponse(UUID id, UUID inquiryId, String content, int regenerationCount) {
        this.id = id;
        this.inquiryId = inquiryId;
        this.content = content;
        this.status = DraftResponseStatus.GENERATED;
        this.regenerationCount = regenerationCount;
        this.createdAt = ZonedDateTime.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getInquiryId() {
        return inquiryId;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public DraftResponseStatus getStatus() {
        return status;
    }

    public void setStatus(DraftResponseStatus status) {
        this.status = status;
    }

    public int getRegenerationCount() {
        return regenerationCount;
    }

    public ZonedDateTime getCreatedAt() {
        return createdAt;
    }
}
