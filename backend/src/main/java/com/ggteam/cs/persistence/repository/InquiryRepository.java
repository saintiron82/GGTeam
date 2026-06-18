package com.ggteam.cs.persistence.repository;

import com.ggteam.cs.common.enums.InquiryStatus;
import com.ggteam.cs.common.enums.InquiryType;
import com.ggteam.cs.common.enums.Urgency;
import com.ggteam.cs.persistence.entity.Inquiry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 문의 저장소. 조회, 상태별 목록, Pull 배정 원자적 갱신(BR-14), 대시보드 검색 제공.
 *
 * <p>담당: 백엔드 A(엔티티/스키마 소유). 워크플로우/대시보드 조회 메서드는 백엔드 C가 추가.
 * 통합 시 합집합으로 유지한다.
 */
public interface InquiryRepository extends JpaRepository<Inquiry, UUID> {

    // --- 백엔드 A ---

    /** 특정 상태의 문의 목록 (접수 시각 오름차순, FIFO — BR-13). */
    List<Inquiry> findByStatusOrderByCreatedAtAsc(InquiryStatus status);

    /** 특정 운영자에게 배정된 문의 목록. */
    List<Inquiry> findByAssignedOperatorId(UUID assignedOperatorId);

    /**
     * Pull 배정 원자적 갱신 (BR-12, BR-14) — 백엔드 A 버전.
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

    // --- 백엔드 C (워크플로우/대시보드) ---

    /** 칸반 보드: 상태별 조회 (종료 SENT 포함, US-20). */
    List<Inquiry> findByStatus(InquiryStatus status);

    /** 상태별 카운트 — 알림 집계 (US-12). */
    long countByStatus(InquiryStatus status);

    /**
     * Pull 배정 후보 조회 (BR-13): PENDING_ASSIGNMENT·미배정.
     * 우선순위: urgency(HIGH→NORMAL→LOW) → createdAt FIFO. AIAnalysis와 left join.
     */
    @Query("""
            select i from Inquiry i
            left join AIAnalysis a on a.inquiryId = i.id
            where i.status = com.ggteam.cs.common.enums.InquiryStatus.PENDING_ASSIGNMENT
              and i.assignedOperatorId is null
            order by case a.urgency
                       when com.ggteam.cs.common.enums.Urgency.HIGH then 0
                       when com.ggteam.cs.common.enums.Urgency.NORMAL then 1
                       else 2 end asc,
                     i.createdAt asc
            """)
    List<Inquiry> findAssignableCandidates(Pageable pageable);

    /**
     * 원자적 조건부 배정 (BR-12, BR-14) — 백엔드 C 버전(claim).
     * @return 1=성공, 0=선점됨
     */
    @Modifying
    @Query("""
            update Inquiry i
               set i.assignedOperatorId = :operatorId,
                   i.status = com.ggteam.cs.common.enums.InquiryStatus.OPERATOR_REVIEWING
             where i.id = :inquiryId
               and i.assignedOperatorId is null
               and i.status = com.ggteam.cs.common.enums.InquiryStatus.PENDING_ASSIGNMENT
            """)
    int claimAssignment(@Param("inquiryId") UUID inquiryId, @Param("operatorId") UUID operatorId);

    /**
     * 필터/검색 (US-22). null 파라미터는 무시. urgency/summary는 AIAnalysis와 left join.
     * keyword는 본문(content) 및 분석 요약(summary)에 부분 일치(대소문자 무시).
     */
    @Query(value = """
            select i from Inquiry i
            left join AIAnalysis a on a.inquiryId = i.id
            where (:status is null or i.status = :status)
              and (:type is null or i.customerType = :type)
              and (:assignee is null or i.assignedOperatorId = :assignee)
              and (:urgency is null or a.urgency = :urgency)
              and (:keyword is null
                   or lower(i.content) like lower(concat('%', :keyword, '%'))
                   or lower(coalesce(a.summary, '')) like lower(concat('%', :keyword, '%')))
              and (:from is null or i.createdAt >= :from)
              and (:to is null or i.createdAt <= :to)
            """,
            countQuery = """
            select count(i) from Inquiry i
            left join AIAnalysis a on a.inquiryId = i.id
            where (:status is null or i.status = :status)
              and (:type is null or i.customerType = :type)
              and (:assignee is null or i.assignedOperatorId = :assignee)
              and (:urgency is null or a.urgency = :urgency)
              and (:keyword is null
                   or lower(i.content) like lower(concat('%', :keyword, '%'))
                   or lower(coalesce(a.summary, '')) like lower(concat('%', :keyword, '%')))
              and (:from is null or i.createdAt >= :from)
              and (:to is null or i.createdAt <= :to)
            """)
    Page<Inquiry> search(@Param("status") InquiryStatus status,
                         @Param("type") InquiryType type,
                         @Param("assignee") UUID assignee,
                         @Param("urgency") Urgency urgency,
                         @Param("keyword") String keyword,
                         @Param("from") ZonedDateTime from,
                         @Param("to") ZonedDateTime to,
                         Pageable pageable);
}
