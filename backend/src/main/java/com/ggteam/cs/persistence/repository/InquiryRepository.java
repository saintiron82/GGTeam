package com.ggteam.cs.persistence.repository;

import com.ggteam.cs.common.enums.InquiryStatus;
import com.ggteam.cs.common.enums.InquiryType;
import com.ggteam.cs.common.enums.Urgency;
import com.ggteam.cs.persistence.entity.Inquiry;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * 문의 Repository.
 *
 * <p><b>엔티티/Repository 소유: 백엔드 A.</b> 백엔드 C 브랜치에서는 워크플로우/대시보드 조회에
 * 필요한 메서드를 stub 선행으로 정의. A 정식 구현과 머지 시 통합한다.
 *
 * <p>대시보드 카드(card)에 필요한 AI 분석 필드(aiType/urgency/summary)는 AIAnalysis와
 * 조인되어야 하므로, 대시보드 조회는 {@code DashboardService}에서 별도 조립한다.
 */
@Repository
public interface InquiryRepository extends JpaRepository<Inquiry, UUID> {

    /**
     * Pull 배정 후보 조회 (BR-13): PENDING_ASSIGNMENT 상태, 미배정.
     * 우선순위는 AIAnalysis.urgency 기준이므로 후보 id를 우선순위 정렬로 조회.
     * urgency 정렬을 위해 ai_analysis와 조인 (HIGH→NORMAL→LOW, 그다음 createdAt FIFO).
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
     * 원자적 조건부 배정 (BR-12, BR-14): 미배정 + PENDING_ASSIGNMENT 인 경우에만 갱신.
     * 갱신된 행 수가 1이면 배정 성공, 0이면 다른 운영자가 선점(ASSIGNMENT_CONFLICT).
     */
    @org.springframework.data.jpa.repository.Modifying
    @Query("""
            update Inquiry i
               set i.assignedOperatorId = :operatorId,
                   i.status = com.ggteam.cs.common.enums.InquiryStatus.OPERATOR_REVIEWING
             where i.id = :inquiryId
               and i.assignedOperatorId is null
               and i.status = com.ggteam.cs.common.enums.InquiryStatus.PENDING_ASSIGNMENT
            """)
    int claimAssignment(@Param("inquiryId") UUID inquiryId, @Param("operatorId") UUID operatorId);

    /** 칸반 보드: 상태별 조회용 전체 (종료 SENT 포함). */
    List<Inquiry> findByStatus(InquiryStatus status);

    /** 미배정(PENDING_ASSIGNMENT) 카운트 — 알림 집계. */
    long countByStatus(InquiryStatus status);

    /**
     * 필터/검색 (US-22). null 파라미터는 무시. urgency/summary는 AIAnalysis와 left join.
     * keyword는 문의 본문(content) 및 분석 요약(summary)에 대해 부분 일치(대소문자 무시).
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
