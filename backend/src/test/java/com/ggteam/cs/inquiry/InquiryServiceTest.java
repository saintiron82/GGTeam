package com.ggteam.cs.inquiry;

import com.ggteam.cs.aipipeline.AIAnalysisService;
import com.ggteam.cs.common.BusinessException;
import com.ggteam.cs.common.ErrorCode;
import com.ggteam.cs.common.enums.InquiryStatus;
import com.ggteam.cs.common.enums.InquiryType;
import com.ggteam.cs.inquiry.dto.CreateInquiryRequest;
import com.ggteam.cs.inquiry.dto.CreateInquiryResponse;
import com.ggteam.cs.persistence.entity.Inquiry;
import com.ggteam.cs.persistence.repository.AIAnalysisRepository;
import com.ggteam.cs.persistence.repository.ApprovalHistoryRepository;
import com.ggteam.cs.persistence.repository.DiagnosisRepository;
import com.ggteam.cs.persistence.repository.DraftResponseRepository;
import com.ggteam.cs.persistence.repository.InquiryRepository;
import com.ggteam.cs.persistence.repository.OperatorRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.ZonedDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * InquiryService 단위 테스트 — 접수 검증/트리거 및 상세 조회 (BR-01, BR-03).
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("InquiryService 접수/조회")
class InquiryServiceTest {

    @Mock InquiryRepository inquiryRepository;
    @Mock AIAnalysisRepository aiAnalysisRepository;
    @Mock DiagnosisRepository diagnosisRepository;
    @Mock DraftResponseRepository draftResponseRepository;
    @Mock ApprovalHistoryRepository approvalHistoryRepository;
    @Mock OperatorRepository operatorRepository;
    @Mock ObjectProvider<AIAnalysisService> aiAnalysisServiceProvider;
    @Mock AIAnalysisService aiAnalysisService;

    InquiryService service;

    @BeforeEach
    void setUp() {
        service = new InquiryService(
                inquiryRepository, aiAnalysisRepository, diagnosisRepository,
                draftResponseRepository, approvalHistoryRepository, operatorRepository,
                aiAnalysisServiceProvider);
    }

    @Test
    @DisplayName("접수 성공: RECEIVED 저장 + AI 분석 트리거")
    void create_success_savesAndTriggers() {
        UUID generatedId = UUID.randomUUID();
        when(inquiryRepository.save(any(Inquiry.class))).thenAnswer(inv -> {
            Inquiry i = inv.getArgument(0);
            ReflectionTestUtils.setField(i, "id", generatedId);
            i.setCreatedAt(ZonedDateTime.now());
            return i;
        });
        when(aiAnalysisServiceProvider.getIfAvailable()).thenReturn(aiAnalysisService);

        CreateInquiryRequest req = new CreateInquiryRequest(
                Map.of("userId", "user-123"), InquiryType.PAYMENT, "결제가 안 되었어요 도와주세요");

        CreateInquiryResponse res = service.create(req);

        assertThat(res.inquiryId()).isEqualTo(generatedId);
        assertThat(res.status()).isEqualTo(InquiryStatus.RECEIVED);
        verify(inquiryRepository).save(any(Inquiry.class));
        verify(aiAnalysisService).analyze(generatedId);
    }

    @Test
    @DisplayName("customerInfo에 userId 누락 시 VALIDATION_ERROR + 저장 안 함 (BR-03)")
    void create_missingUserId_throws() {
        CreateInquiryRequest req = new CreateInquiryRequest(
                Map.of("nickname", "tom"), InquiryType.PAYMENT, "결제가 안 되었어요 도와주세요");

        assertThatThrownBy(() -> service.create(req))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.VALIDATION_ERROR);

        verify(inquiryRepository, never()).save(any());
    }

    @Test
    @DisplayName("AI 분석 서비스 미등록이어도 접수는 성공(트리거 생략)")
    void create_noAiService_stillSucceeds() {
        UUID generatedId = UUID.randomUUID();
        when(inquiryRepository.save(any(Inquiry.class))).thenAnswer(inv -> {
            Inquiry i = inv.getArgument(0);
            ReflectionTestUtils.setField(i, "id", generatedId);
            i.setCreatedAt(ZonedDateTime.now());
            return i;
        });
        when(aiAnalysisServiceProvider.getIfAvailable()).thenReturn(null);

        CreateInquiryRequest req = new CreateInquiryRequest(
                Map.of("userId", "user-9"), InquiryType.PAYMENT, "10자 이상의 문의 내용입니다");

        CreateInquiryResponse res = service.create(req);

        assertThat(res.status()).isEqualTo(InquiryStatus.RECEIVED);
        verify(inquiryRepository).save(any(Inquiry.class));
    }

    @Test
    @DisplayName("상세 조회: 존재하지 않으면 INQUIRY_NOT_FOUND")
    void getDetail_notFound_throws() {
        UUID id = UUID.randomUUID();
        when(inquiryRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getDetail(id))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.INQUIRY_NOT_FOUND);
    }

    @Test
    @DisplayName("상세 조회: 분석 전이면 분석/진단/초안 섹션은 null, 이력 빈 목록")
    void getDetail_minimal_nullSections() {
        UUID id = UUID.randomUUID();
        Inquiry inquiry = new Inquiry();
        ReflectionTestUtils.setField(inquiry, "id", id);
        inquiry.setCustomerInfo(Map.of("userId", "u1"));
        inquiry.setCustomerType(InquiryType.PAYMENT);
        inquiry.setContent("문의 내용 열자 이상");
        inquiry.setStatus(InquiryStatus.RECEIVED);
        inquiry.setCreatedAt(ZonedDateTime.now());

        when(inquiryRepository.findById(id)).thenReturn(Optional.of(inquiry));
        when(aiAnalysisRepository.findByInquiryId(id)).thenReturn(Optional.empty());
        when(diagnosisRepository.findByInquiryId(id)).thenReturn(Optional.empty());
        when(draftResponseRepository.findFirstByInquiryIdOrderByCreatedAtDesc(id)).thenReturn(Optional.empty());
        when(approvalHistoryRepository.findByInquiryIdOrderByTimestampAsc(id)).thenReturn(java.util.List.of());

        var detail = service.getDetail(id);

        assertThat(detail.inquiry().inquiryId()).isEqualTo(id);
        assertThat(detail.inquiry().assignedOperator()).isNull();
        assertThat(detail.analysis()).isNull();
        assertThat(detail.diagnosis()).isNull();
        assertThat(detail.currentDraft()).isNull();
        assertThat(detail.history()).isEmpty();
    }
}
