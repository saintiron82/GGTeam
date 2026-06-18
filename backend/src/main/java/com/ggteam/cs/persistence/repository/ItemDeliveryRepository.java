package com.ggteam.cs.persistence.repository;

import com.ggteam.cs.persistence.entity.ItemDelivery;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

/**
 * [데모 더미] 아이템 지급 저장소. userId/paymentId로 조회.
 *
 * <p>엔티티/스키마 소유: 백엔드 A. 사용: 백엔드 B.
 */
public interface ItemDeliveryRepository extends JpaRepository<ItemDelivery, UUID> {

    /** 고객의 지급 내역 (최신순). */
    List<ItemDelivery> findByUserIdOrderByCreatedAtDesc(String userId);

    /** 결제 건에 연관된 지급 내역. */
    List<ItemDelivery> findByPaymentId(UUID paymentId);
}
