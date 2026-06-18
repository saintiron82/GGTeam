package com.ggteam.cs.persistence.entity;

import com.ggteam.cs.common.enums.DemoEnums.AccountStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.ZonedDateTime;

/**
 * [데모 더미] 계정. 사내 계정 시스템 모사 테이블 (domain-entities.md §3.3).
 * userId로 고객 기준 조회. 운영 연동 시 대체.
 *
 * <p>엔티티/스키마 소유: 백엔드 A. 조회 로직: 백엔드 B.
 */
@Entity
@Table(name = "account")
@Getter
@Setter
@NoArgsConstructor
public class Account extends BaseEntity {

    /** 고객 식별자. UNIQUE (인덱스). */
    @Column(name = "user_id", nullable = false, unique = true, length = 100)
    private String userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private AccountStatus status;

    /** 마지막 로그인 시각 (KST, nullable). */
    @Column(name = "last_login")
    private ZonedDateTime lastLogin;
}
