package com.ggteam.cs.notification;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ggteam.cs.common.BusinessException;
import com.ggteam.cs.common.enums.ApprovalAction;
import com.ggteam.cs.common.enums.InquiryStatus;
import com.ggteam.cs.common.enums.InquiryType;
import com.ggteam.cs.persistence.entity.DraftResponse;
import com.ggteam.cs.persistence.entity.Inquiry;
import com.ggteam.cs.persistence.repository.DraftResponseRepository;
import com.ggteam.cs.persistence.repository.InquiryRepository;
import com.ggteam.cs.workflow.InquiryStateMachine;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * NotificationServiceImpl 단위 테스트. 발송 정책(BR-23~25a) 검증.
 */
@ExtendWith(MockitoExtension.class)
class NotificationServiceImplTest {

    @Mock InquiryRepository inquiryRepository;
    @Mock DraftResponseRepository draftResponseRepository;
    @Mock InquiryStateMachine stateMachine;
    @Mock NotificationChannel channel;

    private UUID inquiryId;

    @BeforeEach
    void setUp() {
        inquiryId = UUID.randomUUID();
    }

    private NotificationServiceImpl service(int maxRetries) {
        return new NotificationServiceImpl(inquiryRepository, draftResponseRepository,
                stateMachine, channel, maxRetries);
    }

    private Inquiry inquiry(InquiryStatus status) {
        Inquiry inquiry = Inquiry.of(Map.<String, Object>of("userId", "u1"), InquiryType.PAYMENT,
                "결제 문의입니다. 확인 부탁드립니다.");
        ReflectionTestUtils.setField(inquiry, "id", inquiryId);
        inquiry.setStatus(status);
        return inquiry;
    }

    @Test
    @DisplayName("발송 성공 시 SENT 전이 (BR-25)")
    void send_success_transitionsToSent() {
        when(inquiryRepository.findById(inquiryId)).thenReturn(Optional.of(inquiry(InquiryStatus.APPROVED)));
        when(draftResponseRepository.findTopByInquiryIdOrderByCreatedAtDesc(inquiryId))
                .thenReturn(Optional.of(DraftResponse.of(inquiryId, "승인된 답변", 0)));
        doNothing().when(channel).dispatch(eq(inquiryId), any());

        service(3).send(inquiryId);

        verify(channel, times(1)).dispatch(eq(inquiryId), any());
        verify(stateMachine).transition(eq(inquiryId), eq(InquiryStatus.SENT),
                eq(ApprovalAction.APPROVE), any());
    }

    @Test
    @DisplayName("APPROVED 아닌 상태에서 발송 시 INVALID_STATE_TRANSITION (BR-24)")
    void send_notApproved_throws() {
        when(inquiryRepository.findById(inquiryId))
                .thenReturn(Optional.of(inquiry(InquiryStatus.OPERATOR_REVIEWING)));

        assertThatThrownBy(() -> service(3).send(inquiryId)).isInstanceOf(BusinessException.class);
        verify(channel, never()).dispatch(any(), any());
    }

    @Test
    @DisplayName("발송 재시도 후 성공: 첫 시도 실패, 두 번째 성공 (BR-25a)")
    void send_retriesThenSucceeds() {
        when(inquiryRepository.findById(inquiryId)).thenReturn(Optional.of(inquiry(InquiryStatus.APPROVED)));
        when(draftResponseRepository.findTopByInquiryIdOrderByCreatedAtDesc(inquiryId))
                .thenReturn(Optional.of(DraftResponse.of(inquiryId, "승인된 답변", 0)));
        doThrow(new RuntimeException("일시 오류")).doNothing().when(channel).dispatch(eq(inquiryId), any());

        service(3).send(inquiryId);

        verify(channel, times(2)).dispatch(eq(inquiryId), any());
        verify(stateMachine).transition(eq(inquiryId), eq(InquiryStatus.SENT), any(), any());
    }

    @Test
    @DisplayName("재시도 한도 초과 시 발송 실패 예외, SENT 전이 없음 (BR-25a)")
    void send_exhaustsRetries_throws() {
        when(inquiryRepository.findById(inquiryId)).thenReturn(Optional.of(inquiry(InquiryStatus.APPROVED)));
        when(draftResponseRepository.findTopByInquiryIdOrderByCreatedAtDesc(inquiryId))
                .thenReturn(Optional.of(DraftResponse.of(inquiryId, "승인된 답변", 0)));
        doThrow(new RuntimeException("지속 오류")).when(channel).dispatch(eq(inquiryId), any());

        assertThatThrownBy(() -> service(3).send(inquiryId)).isInstanceOf(BusinessException.class);

        verify(channel, times(3)).dispatch(eq(inquiryId), any());
        verify(stateMachine, never()).transition(any(), eq(InquiryStatus.SENT), any(), any());
    }
}
