package com.ggteam.cs.persistence.repository;

import com.ggteam.cs.persistence.entity.Diagnosis;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * 진단 저장소. 문의당 1건(UNIQUE, BR-39).
 *
 * <p>엔티티/스키마 소유: 백엔드 A. 사용: 백엔드 B.
 */
public interface DiagnosisRepository extends JpaRepository<Diagnosis, UUID> {

    /** 문의 id로 진단 조회 (1:1). */
    Optional<Diagnosis> findByInquiryId(UUID inquiryId);
}
