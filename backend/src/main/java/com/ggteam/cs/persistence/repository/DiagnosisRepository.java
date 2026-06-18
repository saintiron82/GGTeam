package com.ggteam.cs.persistence.repository;

import com.ggteam.cs.persistence.entity.Diagnosis;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * 진단 Repository.
 *
 * <p><b>엔티티 소유: 백엔드 B.</b> 백엔드 C 브랜치에서는 상세 조립용으로 stub 선행 정의.
 */
@Repository
public interface DiagnosisRepository extends JpaRepository<Diagnosis, UUID> {

    Optional<Diagnosis> findByInquiryId(UUID inquiryId);
}
