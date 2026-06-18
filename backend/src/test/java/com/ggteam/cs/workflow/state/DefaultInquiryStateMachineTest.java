package com.ggteam.cs.workflow.state;

import com.ggteam.cs.common.BusinessException;
import com.ggteam.cs.common.ErrorCode;
import com.ggteam.cs.common.enums.ApprovalAction;
import com.ggteam.cs.common.enums.InquiryStatus;
import com.ggteam.cs.persistence.entity.Inquiry;
import com.ggteam.cs.persistence.repository.InquiryRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * DefaultInquiryStateMachine 단위 테스트 — 상태 전이 규칙 (BR-07~10).
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("InquiryStateMachine 상태 전이")
class DefaultInquiryStateMachineTest {

    @Mock InquiryRepository inquiryRepository;

    DefaultInquiryStateMachine stateMachine() {
        return new DefaultInquiryStateMachine(inquiryRepository);
    }

    // ----- canTransition (순수 검증, 저장소 불필요) -----

    @ParameterizedTest(name = "허용: {0} -> {1}")
    @CsvSource({
            "RECEIVED, AI_ANALYZING",
            "AI_ANALYZING, PENDING_ASSIGNMENT",
            "AI_ANALYZING, MANUAL_CLASSIFICATION_PENDING",
            "PENDING_ASSIGNMENT, OPERATOR_REVIEWING",
            "PENDING_ASSIGNMENT, AI_ANALYZING",
            "OPERATOR_REVIEWING, OPERATOR_REVIEWING",
            "OPERATOR_REVIEWING, APPROVED",
            "OPERATOR_REVIEWING, AI_ANALYZING",
            "APPROVED, SENT",
            "MANUAL_CLASSIFICATION_PENDING, OPERATOR_REVIEWING",
            "MANUAL_CLASSIFICATION_PENDING, PENDING_ASSIGNMENT",
            "MANUAL_CLASSIFICATION_PENDING, AI_ANALYZING"
    })
    @DisplayName("전이 테이블에 정의된 전이는 허용")
    void canTransition_allowed(InquiryStatus from, InquiryStatus to) {
        assertThat(stateMachine().canTransition(from, to)).isTrue();
    }

    @ParameterizedTest(name = "금지: {0} -> {1}")
    @CsvSource({
            "RECEIVED, APPROVED",
            "RECEIVED, SENT",
            "AI_ANALYZING, SENT",
            "PENDING_ASSIGNMENT, APPROVED",
            "APPROVED, OPERATOR_REVIEWING",
            "SENT, AI_ANALYZING",
            "SENT, OPERATOR_REVIEWING",
            "SENT, SENT"
    })
    @DisplayName("정의되지 않은 전이는 금지 (BR-07)")
    void canTransition_forbidden(InquiryStatus from, InquiryStatus to) {
        assertThat(stateMachine().canTransition(from, to)).isFalse();
    }

    @Test
    @DisplayName("SENT는 종료 상태 — 어떤 전이도 불가 (BR-08)")
    void sent_isTerminal() {
        DefaultInquiryStateMachine sm = stateMachine();
        for (InquiryStatus to : InquiryStatus.values()) {
            assertThat(sm.canTransition(InquiryStatus.SENT, to)).isFalse();
        }
    }

    @Test
    @DisplayName("null 입력은 false")
    void canTransition_null_false() {
        DefaultInquiryStateMachine sm = stateMachine();
        assertThat(sm.canTransition(null, InquiryStatus.AI_ANALYZING)).isFalse();
        assertThat(sm.canTransition(InquiryStatus.RECEIVED, null)).isFalse();
    }

    // ----- transition (저장소 연동) -----

    @Test
    @DisplayName("허용 전이 시 상태 변경 + 저장")
    void transition_valid_appliesAndSaves() {
        UUID id = UUID.randomUUID();
        Inquiry inquiry = new Inquiry();
        inquiry.setStatus(InquiryStatus.RECEIVED);
        when(inquiryRepository.findById(id)).thenReturn(Optional.of(inquiry));

        stateMachine().transition(id, InquiryStatus.AI_ANALYZING, null, null);

        assertThat(inquiry.getStatus()).isEqualTo(InquiryStatus.AI_ANALYZING);
        verify(inquiryRepository).save(inquiry);
    }

    @Test
    @DisplayName("금지 전이 시 INVALID_STATE_TRANSITION + 저장 안 함")
    void transition_invalid_throwsAndNoSave() {
        UUID id = UUID.randomUUID();
        Inquiry inquiry = new Inquiry();
        inquiry.setStatus(InquiryStatus.SENT);
        when(inquiryRepository.findById(id)).thenReturn(Optional.of(inquiry));

        assertThatThrownBy(() ->
                stateMachine().transition(id, InquiryStatus.AI_ANALYZING, ApprovalAction.REANALYZE, UUID.randomUUID()))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_STATE_TRANSITION);

        assertThat(inquiry.getStatus()).isEqualTo(InquiryStatus.SENT);
        verify(inquiryRepository, never()).save(inquiry);
    }

    @Test
    @DisplayName("존재하지 않는 문의 → INQUIRY_NOT_FOUND")
    void transition_notFound_throws() {
        UUID id = UUID.randomUUID();
        when(inquiryRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                stateMachine().transition(id, InquiryStatus.AI_ANALYZING, null, null))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.INQUIRY_NOT_FOUND);
    }
}
