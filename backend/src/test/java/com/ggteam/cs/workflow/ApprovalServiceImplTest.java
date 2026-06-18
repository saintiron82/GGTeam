package com.ggteam.cs.workflow;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ggteam.cs.aipipeline.AIAnalysisService;
import com.ggteam.cs.aipipeline.DraftResponseService;
import com.ggteam.cs.common.BusinessException;
import com.ggteam.cs.common.ErrorCode;
import com.ggteam.cs.common.enums.ApprovalAction;
import com.ggteam.cs.common.enums.InquiryStatus;
import com.ggteam.cs.common.enums.InquiryType;
import com.ggteam.cs.notification.NotificationService;
import com.ggteam.cs.persistence.entity.ApprovalHistory;
import com.ggteam.cs.persistence.entity.DraftResponse;
import com.ggteam.cs.persistence.entity.Inquiry;
import com.ggteam.cs.persistence.repository.ApprovalHistoryRepository;
import com.ggteam.cs.persistence.repository.DraftResponseRepository;
import com.ggteam.cs.persistence.repository.InquiryRepository;
import com.ggteam.cs.workflow.dto.OperatorResponses.NewDraft;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * ApprovalServiceImpl 단위 테스트. 워크플로우 비즈니스 규칙(BR-12~22, BR-30a~c) 검증.
 */
@ExtendWith(MockitoExtension.class)
class ApprovalServiceImplTest {

    @Mock InquiryRepository inquiryRepository;
    @Mock DraftResponseRepository draftResponseRepository;
    @Mock ApprovalHistoryRepository approvalHistoryRepository;
    @Mock InquiryStateMachine stateMachine;
    @Mock DraftResponseService draftResponseService;
    @Mock AIAnalysisService aiAnalysisService;
    @Mock NotificationService notificationService;

    @InjectMocks ApprovalServiceImpl service;

    private UUID operatorId;
    private UUID inquiryId;

    @BeforeEach
    void setUp() {
        operatorId = UUID.randomUUID();
        inquiryId = UUID.randomUUID();
    }

    private Inquiry inquiry(InquiryStatus status) {
        Inquiry inquiry = new Inquiry(inquiryId, Map.of("userId", "u1"), InquiryType.PAYMENT,
                "결제가 안 됩니다. 도와주세요.");
        inquiry.setStatus(status);
        return inquiry;
    }

    @Test
    @DisplayName("Pull 배정: 첫 후보 선점 성공 시 ASSIGN 이력 기록 후 inquiryId 반환 (BR-15)")
    void pullAssign_success() {
        Inquiry candidate = inquiry(InquiryStatus.PENDING_ASSIGNMENT);
        when(inquiryRepository.findAssignableCandidates(any())).thenReturn(List.of(candidate));
        when(inquiryRepository.claimAssignment(inquiryId, operatorId)).thenReturn(1);

        Optional<UUID> result = service.pullAssign(operatorId);

        assertThat(result).contains(inquiryId);
        ArgumentCaptor<ApprovalHistory> captor = ArgumentCaptor.forClass(ApprovalHistory.class);
        verify(approvalHistoryRepository).save(captor.capture());
        assertThat(captor.getValue().getAction()).isEqualTo(ApprovalAction.ASSIGN);
    }

    @Test
    @DisplayName("Pull 배정 동시성: 첫 후보 선점 실패(0행) 시 다음 후보로 넘어가 배정 (BR-12, BR-14)")
    void pullAssign_concurrentConflict_fallsThroughToNextCandidate() {
        Inquiry first = inquiry(InquiryStatus.PENDING_ASSIGNMENT);
        UUID secondId = UUID.randomUUID();
        Inquiry second = new Inquiry(secondId, Map.of("userId", "u2"), InquiryType.PAYMENT,
                "두 번째 문의입니다. 확인 바랍니다.");
        second.setStatus(InquiryStatus.PENDING_ASSIGNMENT);

        when(inquiryRepository.findAssignableCandidates(any())).thenReturn(List.of(first, second));
        when(inquiryRepository.claimAssignment(inquiryId, operatorId)).thenReturn(0); // 선점됨
        when(inquiryRepository.claimAssignment(secondId, operatorId)).thenReturn(1);  // 성공

        Optional<UUID> result = service.pullAssign(operatorId);

        assertThat(result).contains(secondId);
        verify(inquiryRepository).claimAssignment(inquiryId, operatorId);
        verify(inquiryRepository).claimAssignment(secondId, operatorId);
    }

    @Test
    @DisplayName("Pull 배정: 가용 문의 없으면 empty 반환 (204)")
    void pullAssign_noCandidate_returnsEmpty() {
        when(inquiryRepository.findAssignableCandidates(any())).thenReturn(List.of());
        assertThat(service.pullAssign(operatorId)).isEmpty();
        verify(approvalHistoryRepository, never()).save(any());
    }

    @Test
    @DisplayName("승인: 활성 초안을 APPROVED로 확정하고 SENT 발송 트리거 (BR-21, BR-23)")
    void approve_triggersDispatch() {
        Inquiry inquiry = inquiry(InquiryStatus.OPERATOR_REVIEWING);
        DraftResponse draft = new DraftResponse(UUID.randomUUID(), inquiryId, "답변 초안", 0);
        when(inquiryRepository.findById(inquiryId)).thenReturn(Optional.of(inquiry));
        when(draftResponseRepository.findTopByInquiryIdOrderByCreatedAtDesc(inquiryId))
                .thenReturn(Optional.of(draft));

        service.approve(inquiryId, operatorId, null);

        verify(stateMachine).transition(inquiryId, InquiryStatus.APPROVED, ApprovalAction.APPROVE, operatorId);
        verify(notificationService).send(inquiryId);
    }

    @Test
    @DisplayName("승인: 활성 초안이 없으면 INVALID_STATE_TRANSITION (BR-21)")
    void approve_noDraft_throws() {
        when(inquiryRepository.findById(inquiryId)).thenReturn(Optional.of(inquiry(InquiryStatus.OPERATOR_REVIEWING)));
        when(draftResponseRepository.findTopByInquiryIdOrderByCreatedAtDesc(inquiryId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.approve(inquiryId, operatorId, null))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.INVALID_STATE_TRANSITION);
        verify(notificationService, never()).send(any());
    }

    @Test
    @DisplayName("반려: 사유 없으면 REASON_REQUIRED (BR-16)")
    void reject_blankReason_throws() {
        assertThatThrownBy(() -> service.reject(inquiryId, operatorId, "  "))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.REASON_REQUIRED);
        verify(draftResponseService, never()).regenerate(any(), any());
    }

    @Test
    @DisplayName("반려: REJECT/REGENERATE 이력 기록 후 새 초안 반환 (BR-17, BR-22)")
    void reject_recordsHistoryAndReturnsNewDraft() {
        UUID newDraftId = UUID.randomUUID();
        DraftResponse newDraft = new DraftResponse(newDraftId, inquiryId, "재생성된 초안", 1);
        when(inquiryRepository.findById(inquiryId)).thenReturn(Optional.of(inquiry(InquiryStatus.OPERATOR_REVIEWING)));
        when(draftResponseService.regenerate(eq(inquiryId), any())).thenReturn(newDraftId);
        when(draftResponseRepository.findById(newDraftId)).thenReturn(Optional.of(newDraft));

        NewDraft result = service.reject(inquiryId, operatorId, "톤이 부적절합니다");

        assertThat(result.draftId()).isEqualTo(newDraftId);
        assertThat(result.regenerationCount()).isEqualTo(1);
        verify(approvalHistoryRepository, times(2)).save(any()); // REJECT + REGENERATE
    }

    @Test
    @DisplayName("재분석: AI_ANALYZING 전이 + REANALYZE 이력 + 분석 트리거 (BR-30a~c)")
    void reanalyze_transitionsAndTriggers() {
        when(inquiryRepository.findById(inquiryId))
                .thenReturn(Optional.of(inquiry(InquiryStatus.OPERATOR_REVIEWING)));

        service.reanalyze(inquiryId, operatorId, "분류 오류");

        verify(stateMachine).transition(inquiryId, InquiryStatus.AI_ANALYZING, ApprovalAction.REANALYZE, operatorId);
        verify(aiAnalysisService).analyze(inquiryId);
        ArgumentCaptor<ApprovalHistory> captor = ArgumentCaptor.forClass(ApprovalHistory.class);
        verify(approvalHistoryRepository).save(captor.capture());
        assertThat(captor.getValue().getAction()).isEqualTo(ApprovalAction.REANALYZE);
    }
}
