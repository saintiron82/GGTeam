package com.ggteam.cs.persistence.repository;

import com.ggteam.cs.persistence.entity.ApprovalHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

/**
 * 승인/처리 이력 저장소. append-only (BR-40) — 저장/조회만, 수정/삭제 미사용.
 *
 * <p>엔티티/스키마 소유: 백엔드 A. 사용: 백엔드 C.
 */
public interface ApprovalHistoryRepository extends JpaRepository<ApprovalHistory, UUID> {

    /** 문의의 처리 이력 (시각 오름차순 — 타임라인 순서). */
    List<ApprovalHistory> findByInquiryIdOrderByTimestampAsc(UUID inquiryId);
}
