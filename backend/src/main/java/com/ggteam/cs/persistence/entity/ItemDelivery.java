package com.ggteam.cs.persistence.entity;

import com.ggteam.cs.common.enums.DemoEnums.DeliveryStatus;
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
 * [데모 더미] 아이템 지급. 사내 지급 시스템 모사 테이블 (domain-entities.md §3.2).
 * paymentId로 결제와 1:N 연결. userId로 고객 기준 조회. 운영 연동 시 대체.
 *
 * <p>엔티티/스키마 소유: 백엔드 A. 조회 로직: 백엔드 B.
 */
@Entity
@Table(name = "item_delivery")
@Getter
@Setter
@NoArgsConstructor
public class ItemDelivery extends BaseEntity {

    /** 연관 결제 (1:N, nullable). FK→payment.id. */
    @Column(name = "payment_id")
    private UUID paymentId;

    /** 고객 식별자 (인덱스). */
    @Column(name = "user_id", nullable = false, length = 100)
    private String userId;

    /** 지급 아이템 식별자. */
    @Column(name = "item_id", nullable = false, length = 100)
    private String itemId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private DeliveryStatus status;

    /** 지급 시각 (KST). */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private ZonedDateTime createdAt;
}
