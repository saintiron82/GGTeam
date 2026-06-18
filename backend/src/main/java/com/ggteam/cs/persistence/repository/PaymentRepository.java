package com.ggteam.cs.persistence.repository;

import com.ggteam.cs.persistence.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

/**
 * [데모 더미] 결제 저장소. PaymentQueryStrategy가 userId로 조회.
 *
 * <p>엔티티/스키마 소유: 백엔드 A. 사용: 백엔드 B.
 */
public interface PaymentRepository extends JpaRepository<Payment, UUID> {

    /** 고객의 결제 내역 (최신순). */
    List<Payment> findByUserIdOrderByCreatedAtDesc(String userId);
}
