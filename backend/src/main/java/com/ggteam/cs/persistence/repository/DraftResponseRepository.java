package com.ggteam.cs.persistence.repository;

import com.ggteam.cs.persistence.entity.DraftResponse;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * 답변 초안 Repository.
 *
 * <p><b>엔티티 소유: 백엔드 B.</b> 백엔드 C 브랜치에서는 승인/수정 워크플로우용으로 stub 선행 정의.
 */
@Repository
public interface DraftResponseRepository extends JpaRepository<DraftResponse, UUID> {

    /** 문의의 활성(current) 초안 = 가장 최근 생성본 (BR-21). */
    Optional<DraftResponse> findTopByInquiryIdOrderByCreatedAtDesc(UUID inquiryId);
}
