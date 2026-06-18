package com.ggteam.cs.persistence.repository;

import com.ggteam.cs.persistence.entity.ApprovalHistory;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * 처리 이력 Repository (append-only, BR-40).
 *
 * <p><b>소유: 백엔드 C.</b>
 */
@Repository
public interface ApprovalHistoryRepository extends JpaRepository<ApprovalHistory, UUID> {

    /** 타임라인 조회 (US-18, US-21): 오래된 순. */
    List<ApprovalHistory> findByInquiryIdOrderByTimestampAsc(UUID inquiryId);
}
