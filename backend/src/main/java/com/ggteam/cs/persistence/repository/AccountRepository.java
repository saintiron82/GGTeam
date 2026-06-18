package com.ggteam.cs.persistence.repository;

import com.ggteam.cs.persistence.entity.Account;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * [데모 더미] 계정 저장소. userId로 조회 (UNIQUE).
 *
 * <p>엔티티/스키마 소유: 백엔드 A. 사용: 백엔드 B.
 */
public interface AccountRepository extends JpaRepository<Account, UUID> {

    /** 고객 식별자로 계정 조회. */
    Optional<Account> findByUserId(String userId);
}
