package com.ggteam.cs.persistence.entity;

import com.ggteam.cs.common.enums.OperatorRole;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;

/**
 * 운영자(상담 운영자/관리자) 엔티티. (domain-entities §2.6)
 *
 * <p><b>소유: 백엔드 A.</b> 백엔드 C 브랜치에서는 병렬 개발(stub 선행)을 위해
 * 계약(domain-entities.md) 기준으로 우선 작성한다. A의 정식 구현 머지 시 대체된다.
 */
@Entity
@Table(name = "operator")
public class Operator {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OperatorRole role = OperatorRole.OPERATOR;

    @Column(name = "failed_login_count", nullable = false)
    private int failedLoginCount = 0;

    @Column(nullable = false)
    private boolean locked = false;

    protected Operator() {}

    public Operator(UUID id, String username, String passwordHash, OperatorRole role) {
        this.id = id;
        this.username = username;
        this.passwordHash = passwordHash;
        this.role = role;
    }

    public UUID getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public OperatorRole getRole() {
        return role;
    }

    public int getFailedLoginCount() {
        return failedLoginCount;
    }

    public boolean isLocked() {
        return locked;
    }

    public void setLocked(boolean locked) {
        this.locked = locked;
    }

    public void setFailedLoginCount(int failedLoginCount) {
        this.failedLoginCount = failedLoginCount;
    }
}
