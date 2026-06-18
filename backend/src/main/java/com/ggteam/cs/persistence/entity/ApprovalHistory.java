package com.ggteam.cs.persistence.entity;

import com.ggteam.cs.common.enums.ApprovalAction;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.ZonedDateTime;
import java.util.UUID;

/**
 * 운영자 처리 이력. (domain-entities §2.5) append-only (BR-40).
 *
 * <p><b>소유: 백엔드 C.</b> 워크플로우 모든 액션을 불변 기록 (BR-22).
 */
@Entity
@Table(name = "approval_history")
public class ApprovalHistory {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(name = "inquiry_id", nullable = false)
    private UUID inquiryId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ApprovalAction action;

    @Column(name = "operator_id", nullable = false)
    private UUID operatorId;

    @Column(columnDefinition = "text")
    private String reason;

    @Column(nullable = false, updatable = false)
    private ZonedDateTime timestamp;

    protected ApprovalHistory() {}

    public ApprovalHistory(UUID id, UUID inquiryId, ApprovalAction action, UUID operatorId, String reason) {
        this.id = id;
        this.inquiryId = inquiryId;
        this.action = action;
        this.operatorId = operatorId;
        this.reason = reason;
        this.timestamp = ZonedDateTime.now();
    }

    /** append-only 기록 생성 팩토리. */
    public static ApprovalHistory record(UUID inquiryId, ApprovalAction action, UUID operatorId, String reason) {
        return new ApprovalHistory(UUID.randomUUID(), inquiryId, action, operatorId, reason);
    }

    public UUID getId() {
        return id;
    }

    public UUID getInquiryId() {
        return inquiryId;
    }

    public ApprovalAction getAction() {
        return action;
    }

    public UUID getOperatorId() {
        return operatorId;
    }

    public String getReason() {
        return reason;
    }

    public ZonedDateTime getTimestamp() {
        return timestamp;
    }
}
