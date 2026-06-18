package com.ggteam.cs.dashboard;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

import com.ggteam.cs.common.enums.InquiryStatus;
import com.ggteam.cs.common.enums.InquiryType;
import com.ggteam.cs.dashboard.dto.InquiryCard;
import com.ggteam.cs.dashboard.dto.NotificationCounts;
import com.ggteam.cs.persistence.entity.Inquiry;
import com.ggteam.cs.persistence.repository.AIAnalysisRepository;
import com.ggteam.cs.persistence.repository.InquiryRepository;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * DashboardService 단위 테스트. 칸반 그룹/알림 집계(US-20, US-12) 검증.
 */
@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock InquiryRepository inquiryRepository;
    @Mock AIAnalysisRepository aiAnalysisRepository;

    @InjectMocks DashboardService service;

    private Inquiry inquiry(InquiryStatus status) {
        Inquiry inquiry = Inquiry.of(Map.<String, Object>of("userId", "u1"),
                InquiryType.PAYMENT, "결제 관련 문의입니다 도와주세요.");
        inquiry.setStatus(status);
        return inquiry;
    }

    @Test
    @DisplayName("칸반 보드: 6개 상태 컬럼을 모두 포함")
    void getBoard_containsAllStatusColumns() {
        when(inquiryRepository.findByStatus(any())).thenReturn(List.of());

        Map<String, List<InquiryCard>> board = service.getBoard();

        assertThat(board).containsKeys(
                InquiryStatus.RECEIVED.name(),
                InquiryStatus.AI_ANALYZING.name(),
                InquiryStatus.PENDING_ASSIGNMENT.name(),
                InquiryStatus.OPERATOR_REVIEWING.name(),
                InquiryStatus.APPROVED.name(),
                InquiryStatus.SENT.name());
    }

    @Test
    @DisplayName("칸반 카드: 분석 전 문의는 AI 필드가 null")
    void getBoard_cardWithoutAnalysis_hasNullAiFields() {
        Inquiry received = inquiry(InquiryStatus.RECEIVED);
        when(inquiryRepository.findByStatus(InquiryStatus.RECEIVED)).thenReturn(List.of(received));
        when(inquiryRepository.findByStatus(InquiryStatus.AI_ANALYZING)).thenReturn(List.of());
        when(inquiryRepository.findByStatus(InquiryStatus.PENDING_ASSIGNMENT)).thenReturn(List.of());
        when(inquiryRepository.findByStatus(InquiryStatus.OPERATOR_REVIEWING)).thenReturn(List.of());
        when(inquiryRepository.findByStatus(InquiryStatus.APPROVED)).thenReturn(List.of());
        when(inquiryRepository.findByStatus(InquiryStatus.SENT)).thenReturn(List.of());
        when(aiAnalysisRepository.findByInquiryIdIn(anyList())).thenReturn(List.of());

        List<InquiryCard> received_cards = service.getBoard().get(InquiryStatus.RECEIVED.name());

        assertThat(received_cards).hasSize(1);
        assertThat(received_cards.get(0).aiType()).isNull();
        assertThat(received_cards.get(0).urgency()).isNull();
        assertThat(received_cards.get(0).customerType()).isEqualTo(InquiryType.PAYMENT.name());
    }

    @Test
    @DisplayName("알림 집계: 미배정 건수 반영")
    void getNotifications_countsUnassigned() {
        when(inquiryRepository.countByStatus(InquiryStatus.PENDING_ASSIGNMENT)).thenReturn(5L);
        when(inquiryRepository.findByStatus(InquiryStatus.PENDING_ASSIGNMENT)).thenReturn(List.of());

        NotificationCounts counts = service.getNotifications();

        assertThat(counts.unassignedCount()).isEqualTo(5L);
        assertThat(counts.urgentCount()).isEqualTo(0L);
    }
}
