package com.ggteam.cs.persistence.repository;

import com.ggteam.cs.persistence.entity.AIAnalysis;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * AI 분석 결과 Repository.
 *
 * <p><b>엔티티 소유: 백엔드 B.</b> 백엔드 C 브랜치에서는 대시보드 카드/상세 조립용으로 stub 선행 정의.
 */
@Repository
public interface AIAnalysisRepository extends JpaRepository<AIAnalysis, UUID> {

    Optional<AIAnalysis> findByInquiryId(UUID inquiryId);

    /** 대시보드 카드 조립용 배치 조회. */
    List<AIAnalysis> findByInquiryIdIn(List<UUID> inquiryIds);
}
