package com.ggteam.cs.persistence.repository;

import com.ggteam.cs.persistence.entity.Operator;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * 운영자 Repository.
 *
 * <p><b>소유: 백엔드 A.</b> 백엔드 C 브랜치에서는 이력/배정 표시용으로 stub 선행 정의.
 */
@Repository
public interface OperatorRepository extends JpaRepository<Operator, UUID> {

    List<Operator> findByIdIn(List<UUID> ids);
}
