package com.ggteam.cs.persistence.entity;

import com.ggteam.cs.common.enums.InquiryStatus;
import com.ggteam.cs.common.enums.InquiryType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.ZonedDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * 고객 문의 루트 엔티티. 전체 처리 흐름의 중심 (domain-entities.md §2.1).
 *
 * <p>상태 전이는 InquiryStateMachine을 통해서만 수행한다 (BR-09).
 * 배정은 동시에 최대 1명 (BR-12), 미배정 시 assignedOperatorId는 null.
 *
 * <p>담당: 백엔드 A.
 */
@Entity
@Table(name = "inquiry")
@Getter
@Setter
@NoArgsConstructor
public class Inquiry extends BaseEntity {

    /** 고객 식별 정보 (userId, 닉네임, 연락 채널 등). jsonb. */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "customer_info", nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> customerInfo;

    /** 문의 유형. MVP에서는 PAYMENT만 E2E (BR-04). */
    @Enumerated(EnumType.STRING)
    @Column(name = "customer_type", nullable = false, length = 20)
    private InquiryType customerType;

    /** 고객 문의 본문. 최소 10자 (BR-01). */
    @Column(name = "content", nullable = false, columnDefinition = "text")
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 40)
    private InquiryStatus status = InquiryStatus.RECEIVED;

    /** 접수 시각 (KST, 자동 생성). */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private ZonedDateTime createdAt;

    /** 배정된 운영자 id. 미배정 시 null (FK→operator.id). */
    @Column(name = "assigned_operator_id")
    private UUID assignedOperatorId;

    /** 문의 접수 팩토리 (id/createdAt 자동, status=RECEIVED). */
    public static Inquiry of(Map<String, Object> customerInfo, InquiryType customerType, String content) {
        Inquiry i = new Inquiry();
        i.setCustomerInfo(customerInfo);
        i.setCustomerType(customerType);
        i.setContent(content);
        return i;
    }
}
