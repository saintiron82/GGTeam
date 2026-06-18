package com.ggteam.cs.persistence.entity;

import com.ggteam.cs.common.enums.DemoEnums.PaymentStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.ZonedDateTime;

/**
 * [데모 더미] 결제. 사내 결제 시스템 모사 테이블 (domain-entities.md §3.1).
 * SystemDataQueryService(PaymentQueryStrategy)가 userId로 조회. 운영 연동 시 대체.
 *
 * <p>엔티티/스키마 소유: 백엔드 A. 조회 로직: 백엔드 B.
 */
@Entity
@Table(name = "payment")
@Getter
@Setter
@NoArgsConstructor
public class Payment extends BaseEntity {

    /** 고객 식별자 (인덱스). */
    @Column(name = "user_id", nullable = false, length = 100)
    private String userId;

    /** 결제 금액 (>= 0). */
    @Column(name = "amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private PaymentStatus status;

    /** 실패 시 오류 로그. */
    @Column(name = "error_log", columnDefinition = "text")
    private String errorLog;

    /** 결제 시각 (KST). */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private ZonedDateTime createdAt;
}
