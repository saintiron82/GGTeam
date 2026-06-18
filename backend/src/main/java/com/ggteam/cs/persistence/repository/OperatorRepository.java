package com.ggteam.cs.persistence.repository;

import com.ggteam.cs.persistence.entity.Operator;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 운영자 저장소. 인증(username 조회) 및 계정 관리에 사용.
 *
 * <p>담당: 백엔드 A.
 */
public interface OperatorRepository extends JpaRepository<Operator, UUID> {

    /** 로그인 ID로 운영자 조회 (인증, BR-06). */
    Optional<Operator> findByUsername(String username);

    boolean existsByUsername(String username);

    /** 여러 운영자 일괄 조회 (이력 타임라인 표시명 해석용). */
    List<Operator> findByIdIn(List<UUID> ids);
}
