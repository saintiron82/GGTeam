package com.ggteam.cs.persistence.entity;

import com.ggteam.cs.common.enums.ApprovalAction;
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
 * 승인/처리 이력. 운영자의 모든 액션을 불변(append-only)으로 기록 (BR-22, BR-40).
 * (domain-entities.md §2.5)
 *
 * <p>수정/삭제 불가. 반려(REJECT) 시 reason 필수 (BR-16).
 *
 * <p>엔티티/스키마 소유: 백엔드 A. 비즈니스 로직: 백엔드 C.
 */
@Entity
@Table(name = "approval_history")
@Getter
@Setter
@NoArgsConstructor
public class ApprovalHistory extends BaseEntity {

    /** 대상 문의 (N:1). FK→inquiry.id. */
    @Column(name = "inquiry_id", nullable = false)
    private UUID inquiryId;

    @Enumerated(EnumType.STRING)
    @Column(name = "action", nullable = false, length = 20)
    private ApprovalAction action;

    /** 액션 수행 운영자 (N:1). FK→operator.id. */
    @Column(name = "operator_id", nullable = false)
    private UUID operatorId;

    /** 반려 시 필수, 그 외 선택 (BR-16). */
    @Column(name = "reason", columnDefinition = "text")
    private String reason;

    /** 액션 시각 (KST, 자동 생성). */
    @CreationTimestamp
    @Column(name = "timestamp", nullable = false, updatable = false)
    private ZonedDateTime timestamp;

    /** 이력 생성 팩토리 (id/timestamp 자동). 반려 시 reason 필수(BR-16)는 호출측에서 검증. */
    public static ApprovalHistory record(UUID inquiryId, ApprovalAction action, UUID operatorId, String reason) {
        ApprovalHistory h = new ApprovalHistory();
        h.setInquiryId(inquiryId);
        h.setAction(action);
        h.setOperatorId(operatorId);
        h.setReason(reason);
        return h;
    }
}
