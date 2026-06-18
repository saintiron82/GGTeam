package com.ggteam.cs.persistence.entity;

import com.ggteam.cs.common.enums.InquiryStatus;
import com.ggteam.cs.common.enums.InquiryType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.ZonedDateTime;
import java.util.Map;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * 고객 문의 루트 엔티티. (domain-entities §2.1)
 *
 * <p><b>소유: 백엔드 A.</b> 백엔드 C 브랜치에서는 stub 선행으로 계약 기준 우선 작성.
 */
@Entity
@Table(name = "inquiry")
public class Inquiry {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "customer_info", nullable = false)
    private Map<String, String> customerInfo;

    @Enumerated(EnumType.STRING)
    @Column(name = "customer_type", nullable = false)
    private InquiryType customerType;

    @Column(nullable = false, columnDefinition = "text")
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private InquiryStatus status = InquiryStatus.RECEIVED;

    @Column(name = "created_at", nullable = false, updatable = false)
    private ZonedDateTime createdAt;

    @Column(name = "assigned_operator_id")
    private UUID assignedOperatorId;

    /** 낙관적 잠금 — Pull 배정 동시성 보조 (BR-12, BR-14). */
    @Version
    @Column(nullable = false)
    private long version;

    protected Inquiry() {}

    public Inquiry(UUID id, Map<String, String> customerInfo, InquiryType customerType, String content) {
        this.id = id;
        this.customerInfo = customerInfo;
        this.customerType = customerType;
        this.content = content;
        this.status = InquiryStatus.RECEIVED;
        this.createdAt = ZonedDateTime.now();
    }

    public UUID getId() {
        return id;
    }

    public Map<String, String> getCustomerInfo() {
        return customerInfo;
    }

    public InquiryType getCustomerType() {
        return customerType;
    }

    public String getContent() {
        return content;
    }

    public InquiryStatus getStatus() {
        return status;
    }

    public void setStatus(InquiryStatus status) {
        this.status = status;
    }

    public ZonedDateTime getCreatedAt() {
        return createdAt;
    }

    public UUID getAssignedOperatorId() {
        return assignedOperatorId;
    }

    public void setAssignedOperatorId(UUID assignedOperatorId) {
        this.assignedOperatorId = assignedOperatorId;
    }

    public long getVersion() {
        return version;
    }
}
