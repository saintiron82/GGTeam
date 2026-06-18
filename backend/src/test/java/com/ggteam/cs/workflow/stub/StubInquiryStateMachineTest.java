package com.ggteam.cs.workflow.stub;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ggteam.cs.common.BusinessException;
import com.ggteam.cs.common.enums.ApprovalAction;
import com.ggteam.cs.common.enums.InquiryStatus;
import com.ggteam.cs.common.enums.InquiryType;
import com.ggteam.cs.persistence.entity.Inquiry;
import com.ggteam.cs.persistence.repository.InquiryRepository;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * StubInquiryStateMachine 전이 테이블 검증 (BR-07~10). A 정식 구현 전 병렬 개발용 stub의 정합성 확인.
 */
@ExtendWith(MockitoExtension.class)
class StubInquiryStateMachineTest {

    @Mock InquiryRepository inquiryRepository;

    @Test
    @DisplayName("정상 전이 테이블 검증")
    void canTransition_validPaths() {
        StubInquiryStateMachine sm = new StubInquiryStateMachine(inquiryRepository);
        assertThat(sm.canTransition(InquiryStatus.RECEIVED, InquiryStatus.AI_ANALYZING)).isTrue();
        assertThat(sm.canTransition(InquiryStatus.PENDING_ASSIGNMENT, InquiryStatus.OPERATOR_REVIEWING)).isTrue();
        assertThat(sm.canTransition(InquiryStatus.OPERATOR_REVIEWING, InquiryStatus.APPROVED)).isTrue();
        assertThat(sm.canTransition(InquiryStatus.OPERATOR_REVIEWING, InquiryStatus.OPERATOR_REVIEWING)).isTrue();
        assertThat(sm.canTransition(InquiryStatus.OPERATOR_REVIEWING, InquiryStatus.AI_ANALYZING)).isTrue();
        assertThat(sm.canTransition(InquiryStatus.APPROVED, InquiryStatus.SENT)).isTrue();
    }

    @Test
    @DisplayName("SENT는 종료 상태 — 어떤 전이도 불가 (BR-08)")
    void canTransition_sentIsTerminal() {
        StubInquiryStateMachine sm = new StubInquiryStateMachine(inquiryRepository);
        assertThat(sm.canTransition(InquiryStatus.SENT, InquiryStatus.OPERATOR_REVIEWING)).isFalse();
        assertThat(sm.canTransition(InquiryStatus.SENT, InquiryStatus.APPROVED)).isFalse();
    }

    @Test
    @DisplayName("비정상 전이 시 INVALID_STATE_TRANSITION (BR-07)")
    void transition_invalid_throws() {
        UUID id = UUID.randomUUID();
        Inquiry inquiry = Inquiry.of(Map.<String, Object>of("userId", "u1"), InquiryType.PAYMENT, "문의 내용입니다 충분히.");
        ReflectionTestUtils.setField(inquiry, "id", id);
        inquiry.setStatus(InquiryStatus.RECEIVED);
        when(inquiryRepository.findById(id)).thenReturn(Optional.of(inquiry));

        StubInquiryStateMachine sm = new StubInquiryStateMachine(inquiryRepository);

        assertThatThrownBy(() ->
                sm.transition(id, InquiryStatus.APPROVED, ApprovalAction.APPROVE, UUID.randomUUID()))
                .isInstanceOf(BusinessException.class);
        verify(inquiryRepository, never()).save(any());
    }

    @Test
    @DisplayName("정상 전이 시 상태 갱신 및 저장")
    void transition_valid_savesNewStatus() {
        UUID id = UUID.randomUUID();
        Inquiry inquiry = Inquiry.of(Map.<String, Object>of("userId", "u1"), InquiryType.PAYMENT, "문의 내용입니다 충분히.");
        ReflectionTestUtils.setField(inquiry, "id", id);
        inquiry.setStatus(InquiryStatus.PENDING_ASSIGNMENT);
        when(inquiryRepository.findById(id)).thenReturn(Optional.of(inquiry));

        StubInquiryStateMachine sm = new StubInquiryStateMachine(inquiryRepository);
        sm.transition(id, InquiryStatus.OPERATOR_REVIEWING, ApprovalAction.ASSIGN, UUID.randomUUID());

        assertThat(inquiry.getStatus()).isEqualTo(InquiryStatus.OPERATOR_REVIEWING);
        verify(inquiryRepository).save(inquiry);
    }
}
