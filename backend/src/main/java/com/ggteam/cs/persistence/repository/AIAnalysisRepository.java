package com.ggteam.cs.persistence.repository;

import com.ggteam.cs.persistence.entity.AIAnalysis;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * AI 분석 결과 저장소. 문의당 1건(UNIQUE, BR-39).
 *
 * <p>엔티티/스키마 소유: 백엔드 A. 사용: 백엔드 B.
 */
public interface AIAnalysisRepository extends JpaRepository<AIAnalysis, UUID> {

    /** 문의 id로 분석 결과 조회 (1:1). */
    Optional<AIAnalysis> findByInquiryId(UUID inquiryId);
}
