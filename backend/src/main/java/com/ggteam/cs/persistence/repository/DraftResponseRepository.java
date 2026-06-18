package com.ggteam.cs.persistence.repository;

import com.ggteam.cs.persistence.entity.DraftResponse;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 답변 초안 저장소. 동일 문의 내 1:N, 최신 1건이 활성(current) 초안.
 *
 * <p>엔티티/스키마 소유: 백엔드 A. 사용: 백엔드 B/C.
 */
public interface DraftResponseRepository extends JpaRepository<DraftResponse, UUID> {

    /** 문의의 모든 초안 (생성 시각 내림차순 — 최신 우선). */
    List<DraftResponse> findByInquiryIdOrderByCreatedAtDesc(UUID inquiryId);

    /** 활성(current) 초안 = 가장 최근 생성 1건. */
    Optional<DraftResponse> findFirstByInquiryIdOrderByCreatedAtDesc(UUID inquiryId);

    /** 활성(current) 초안 = 가장 최근 생성 1건 (findFirst 동의어, 백엔드 C 사용). */
    Optional<DraftResponse> findTopByInquiryIdOrderByCreatedAtDesc(UUID inquiryId);
}
