package com.ggteam.cs.dashboard;

import com.ggteam.cs.common.enums.InquiryStatus;
import com.ggteam.cs.common.enums.InquiryType;
import com.ggteam.cs.common.enums.Urgency;
import com.ggteam.cs.dashboard.dto.InquiryCard;
import com.ggteam.cs.dashboard.dto.NotificationCounts;
import com.ggteam.cs.dashboard.dto.PageResponse;
import com.ggteam.cs.persistence.entity.AIAnalysis;
import com.ggteam.cs.persistence.entity.Inquiry;
import com.ggteam.cs.persistence.repository.AIAnalysisRepository;
import com.ggteam.cs.persistence.repository.InquiryRepository;
import java.time.ZonedDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 대시보드 조회/집계 (US-20 칸반, US-22 필터/검색, US-12 알림). <b>담당: 백엔드 C.</b>
 *
 * <p>카드(card)는 Inquiry + AIAnalysis를 조립한다. 분석 전 단계는 AI 관련 필드가 null.
 */
@Service
public class DashboardService {

    /** 칸반 보드에 노출할 상태 컬럼 (01-api-contract §4 순서). */
    private static final List<InquiryStatus> BOARD_STATUSES = List.of(
            InquiryStatus.RECEIVED,
            InquiryStatus.AI_ANALYZING,
            InquiryStatus.PENDING_ASSIGNMENT,
            InquiryStatus.OPERATOR_REVIEWING,
            InquiryStatus.APPROVED,
            InquiryStatus.SENT);

    private final InquiryRepository inquiryRepository;
    private final AIAnalysisRepository aiAnalysisRepository;

    public DashboardService(InquiryRepository inquiryRepository, AIAnalysisRepository aiAnalysisRepository) {
        this.inquiryRepository = inquiryRepository;
        this.aiAnalysisRepository = aiAnalysisRepository;
    }

    /** 칸반 보드: 상태별 카드 그룹 (US-20). */
    @Transactional(readOnly = true)
    public Map<String, List<InquiryCard>> getBoard() {
        Map<String, List<InquiryCard>> board = new LinkedHashMap<>();
        for (InquiryStatus status : BOARD_STATUSES) {
            List<Inquiry> inquiries = inquiryRepository.findByStatus(status);
            board.put(status.name(), toCards(inquiries));
        }
        return board;
    }

    /** 목록 조회 (필터/검색/페이징, US-22). */
    @Transactional(readOnly = true)
    public PageResponse<InquiryCard> search(InquiryStatus status, Urgency urgency, InquiryType type,
                                            UUID assignee, String keyword,
                                            ZonedDateTime from, ZonedDateTime to, Pageable pageable) {
        String normalizedKeyword = (keyword == null || keyword.isBlank()) ? null : keyword.trim();
        Page<Inquiry> page = inquiryRepository.search(status, type, assignee, urgency,
                normalizedKeyword, from, to, pageable);
        return PageResponse.from(page, toCards(page.getContent()));
    }

    /** 알림 집계 (US-12): 미배정 건수 + 긴급(HIGH) 미처리 건수. */
    @Transactional(readOnly = true)
    public NotificationCounts getNotifications() {
        long unassigned = inquiryRepository.countByStatus(InquiryStatus.PENDING_ASSIGNMENT);
        long urgent = countUrgentOpen();
        return new NotificationCounts(unassigned, urgent);
    }

    /** 긴급(HIGH) 미처리(미발송) 건수 집계. */
    private long countUrgentOpen() {
        List<Inquiry> pending = inquiryRepository.findByStatus(InquiryStatus.PENDING_ASSIGNMENT);
        List<UUID> ids = pending.stream().map(Inquiry::getId).toList();
        if (ids.isEmpty()) {
            return 0;
        }
        return aiAnalysisRepository.findByInquiryIdIn(ids).stream()
                .filter(a -> a.getUrgency() == Urgency.HIGH)
                .count();
    }

    /** Inquiry 목록 → 카드 목록 (AIAnalysis 배치 조회 조립). */
    private List<InquiryCard> toCards(List<Inquiry> inquiries) {
        if (inquiries.isEmpty()) {
            return List.of();
        }
        List<UUID> ids = inquiries.stream().map(Inquiry::getId).toList();
        Map<UUID, AIAnalysis> analysisByInquiry = aiAnalysisRepository.findByInquiryIdIn(ids).stream()
                .collect(Collectors.toMap(AIAnalysis::getInquiryId, a -> a, (a, b) -> a));

        return inquiries.stream().map(i -> {
            AIAnalysis a = analysisByInquiry.get(i.getId());
            return new InquiryCard(
                    i.getId(),
                    i.getCustomerType().name(),
                    a != null && a.getAiType() != null ? a.getAiType().name() : null,
                    a != null && a.getUrgency() != null ? a.getUrgency().name() : null,
                    a != null ? a.getSummary() : null,
                    i.getStatus().name(),
                    i.getAssignedOperatorId(),
                    i.getCreatedAt());
        }).toList();
    }
}
