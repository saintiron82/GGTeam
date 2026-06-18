package com.ggteam.cs.persistence.repository;

import com.ggteam.cs.common.enums.InquiryStatus;
import com.ggteam.cs.persistence.entity.Inquiry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

/**
 * 문의 저장소. 조회, 상태별 목록, Pull 배정 원자적 갱신(BR-14) 제공.
 *
 * <p>담당: 백엔드 A. 배정 비즈니스 로직(우선순위 선택)은 백엔드 C가 서비스 계층에서 조합.
 */
public interface InquiryRepository extends JpaRepository<Inquiry, UUID> {

    /** 특정 상태의 문의 목록 (접수 시각 오름차순, FIFO — BR-13). */
    List<Inquiry> findByStatusOrderByCreatedAtAsc(InquiryStatus status);

    /** 특정 운영자에게 배정된 문의 목록. */
    List<Inquiry> findByAssignedOperatorId(UUID assignedOperatorId);

    /**
     * Pull 배정 원자적 갱신 (BR-12, BR-14).
     *
     * <p>PENDING_ASSIGNMENT이고 미배정인 문의에 한해 OPERATOR_REVIEWING + 운영자 배정.
     * 영향 행 수 == 1이면 배정 성공, == 0이면 이미 타 운영자가 점유(경쟁 패배).
     * 동시성은 이 조건부 UPDATE로 보장한다.
     *
     * @return 갱신된 행 수 (1=성공, 0=경쟁 실패)
     */
    @Modifying
    @Query("""
            UPDATE Inquiry i
               SET i.status = com.ggteam.cs.common.enums.InquiryStatus.OPERATOR_REVIEWING,
                   i.assignedOperatorId = :operatorId
             WHERE i.id = :inquiryId
               AND i.status = com.ggteam.cs.common.enums.InquiryStatus.PENDING_ASSIGNMENT
               AND i.assignedOperatorId IS NULL
            """)
    int assignAtomically(@Param("inquiryId") UUID inquiryId, @Param("operatorId") UUID operatorId);
}
