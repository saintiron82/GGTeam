package com.ggteam.cs.persistence.entity;

import com.ggteam.cs.common.enums.OperatorRole;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 운영자(상담 운영자/관리자). 시스템 사용자 (domain-entities.md §2.6).
 *
 * <p>비밀번호는 평문 저장 금지, 단방향 해시만 저장 (BR-36).
 * 로그인 5회 실패 시 잠금 (BR-31), 성공 시 실패 카운트 초기화 (BR-33).
 *
 * <p>담당: 백엔드 A.
 */
@Entity
@Table(name = "operator")
@Getter
@Setter
@NoArgsConstructor
public class Operator extends BaseEntity {

    /** 로그인 ID. UNIQUE (BR-06). */
    @Column(name = "username", nullable = false, unique = true, length = 100)
    private String username;

    /** 해시된 비밀번호 (BCrypt). 평문 저장 금지 (BR-36). */
    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    private OperatorRole role = OperatorRole.OPERATOR;

    /** 연속 로그인 실패 횟수. 5회 도달 시 잠금 (BR-31). */
    @Column(name = "failed_login_count", nullable = false)
    private int failedLoginCount = 0;

    /** 계정 잠금 여부 (BR-32). */
    @Column(name = "locked", nullable = false)
    private boolean locked = false;
}
