package com.ggteam.cs.inquiry;

import com.ggteam.cs.aipipeline.AIAnalysisService;
import com.ggteam.cs.auth.dto.OperatorSummary;
import com.ggteam.cs.common.BusinessException;
import com.ggteam.cs.common.ErrorCode;
import com.ggteam.cs.inquiry.dto.CreateInquiryRequest;
import com.ggteam.cs.inquiry.dto.CreateInquiryResponse;
import com.ggteam.cs.inquiry.dto.InquiryDetail;
import com.ggteam.cs.inquiry.dto.InquiryDetail.AnalysisSection;
import com.ggteam.cs.inquiry.dto.InquiryDetail.DiagnosisSection;
import com.ggteam.cs.inquiry.dto.InquiryDetail.DraftSection;
import com.ggteam.cs.inquiry.dto.InquiryDetail.HistoryItem;
import com.ggteam.cs.inquiry.dto.InquiryDetail.InquirySection;
import com.ggteam.cs.common.enums.InquiryStatus;
import com.ggteam.cs.persistence.entity.Inquiry;
import com.ggteam.cs.persistence.entity.Operator;
import com.ggteam.cs.persistence.repository.AIAnalysisRepository;
import com.ggteam.cs.persistence.repository.ApprovalHistoryRepository;
import com.ggteam.cs.persistence.repository.DiagnosisRepository;
import com.ggteam.cs.persistence.repository.DraftResponseRepository;
import com.ggteam.cs.persistence.repository.InquiryRepository;
import com.ggteam.cs.persistence.repository.OperatorRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 문의 접수/조회/상세 조립 서비스 (US-03~05 접수, US-23 상세 조회).
 *
 * <p>접수 시 Inquiry를 RECEIVED로 생성하고 AI 분석을 비동기 트리거한다.
 * 상세 조회 시 문의/분석/진단/초안/이력을 한 응답으로 조립한다.
 *
 * <p>AI 분석 서비스(AIAnalysisService)는 백엔드 B가 구현한다. 백엔드 A 단독 개발/부팅을
 * 가능하게 하기 위해 {@link ObjectProvider}로 느슨하게 주입한다(빈이 있으면 트리거, 없으면 경고).
 *
 * <p>담당: 백엔드 A.
 */
@Service
public class InquiryService {

    private static final Logger log = LoggerFactory.getLogger(InquiryService.class);
    private static final String KEY_USER_ID = "userId";

    private final InquiryRepository inquiryRepository;
    private final AIAnalysisRepository aiAnalysisRepository;
    private final DiagnosisRepository diagnosisRepository;
    private final DraftResponseRepository draftResponseRepository;
    private final ApprovalHistoryRepository approvalHistoryRepository;
    private final OperatorRepository operatorRepository;
    private final ObjectProvider<AIAnalysisService> aiAnalysisServiceProvider;

    public InquiryService(
            InquiryRepository inquiryRepository,
            AIAnalysisRepository aiAnalysisRepository,
            DiagnosisRepository diagnosisRepository,
            DraftResponseRepository draftResponseRepository,
            ApprovalHistoryRepository approvalHistoryRepository,
            OperatorRepository operatorRepository,
            ObjectProvider<AIAnalysisService> aiAnalysisServiceProvider) {
        this.inquiryRepository = inquiryRepository;
        this.aiAnalysisRepository = aiAnalysisRepository;
        this.diagnosisRepository = diagnosisRepository;
        this.draftResponseRepository = draftResponseRepository;
        this.approvalHistoryRepository = approvalHistoryRepository;
        this.operatorRepository = operatorRepository;
        this.aiAnalysisServiceProvider = aiAnalysisServiceProvider;
    }

    /**
     * 문의 접수. BR-01(content 10자)은 DTO 검증, BR-03(userId 포함)은 여기서 검증.
     * 저장(RECEIVED) 후 AI 분석을 비동기 트리거한다.
     */
    @Transactional
    public CreateInquiryResponse create(CreateInquiryRequest request) {
        validateCustomerInfo(request.customerInfo());

        Inquiry inquiry = new Inquiry();
        inquiry.setCustomerInfo(request.customerInfo());
        inquiry.setCustomerType(request.customerType());
        inquiry.setContent(request.content());
        inquiry.setStatus(InquiryStatus.RECEIVED);

        Inquiry saved = inquiryRepository.save(inquiry);
        triggerAiAnalysis(saved.getId());
        return CreateInquiryResponse.from(saved);
    }

    /** BR-03: customerInfo는 userId를 포함해야 한다. */
    private void validateCustomerInfo(Map<String, Object> customerInfo) {
        Object userId = customerInfo == null ? null : customerInfo.get(KEY_USER_ID);
        if (userId == null || userId.toString().isBlank()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "customerInfo.userId는 필수입니다.");
        }
    }

    private void triggerAiAnalysis(UUID inquiryId) {
        AIAnalysisService aiAnalysisService = aiAnalysisServiceProvider.getIfAvailable();
        if (aiAnalysisService == null) {
            log.warn("AIAnalysisService 미등록 — 문의 {} 접수만 저장(분석 미트리거). 백엔드 B 통합 시 자동 연동.", inquiryId);
            return;
        }
        aiAnalysisService.analyze(inquiryId);
    }

    /** 문의 상세 조회 조립 (US-23). 미완료 단계 섹션은 null. */
    @Transactional(readOnly = true)
    public InquiryDetail getDetail(UUID inquiryId) {
        Inquiry inquiry = inquiryRepository.findById(inquiryId)
                .orElseThrow(() -> new BusinessException(ErrorCode.INQUIRY_NOT_FOUND));

        // 운영자 요약 캐시 (history와 assignedOperator 공유, N+1 방지)
        Map<UUID, OperatorSummary> operatorCache = new HashMap<>();

        InquirySection inquirySection = new InquirySection(
                inquiry.getId(),
                inquiry.getCustomerInfo(),
                inquiry.getCustomerType(),
                inquiry.getContent(),
                inquiry.getStatus(),
                inquiry.getCreatedAt(),
                resolveOperator(inquiry.getAssignedOperatorId(), operatorCache));

        AnalysisSection analysisSection = aiAnalysisRepository.findByInquiryId(inquiryId)
                .map(a -> new AnalysisSection(
                        a.getAiType(), a.getSubCategory(), a.getUrgency(), a.getSummary(),
                        a.getKeywords(), a.getSystemQueryResult(), a.getFailureType()))
                .orElse(null);

        DiagnosisSection diagnosisSection = diagnosisRepository.findByInquiryId(inquiryId)
                .map(d -> new DiagnosisSection(d.getCause(), d.getSuggestedDirection(), d.getConfidence()))
                .orElse(null);

        DraftSection draftSection = draftResponseRepository
                .findFirstByInquiryIdOrderByCreatedAtDesc(inquiryId)
                .map(d -> new DraftSection(d.getId(), d.getContent(), d.getStatus(), d.getRegenerationCount()))
                .orElse(null);

        List<HistoryItem> history = approvalHistoryRepository
                .findByInquiryIdOrderByTimestampAsc(inquiryId).stream()
                .map(h -> new HistoryItem(
                        h.getAction(),
                        resolveOperator(h.getOperatorId(), operatorCache),
                        h.getReason(),
                        h.getTimestamp()))
                .toList();

        return new InquiryDetail(inquirySection, analysisSection, diagnosisSection, draftSection, history);
    }

    /** operatorId를 OperatorSummary로 해석. null이거나 미존재 시 null. 캐시로 중복 조회 방지. */
    private OperatorSummary resolveOperator(UUID operatorId, Map<UUID, OperatorSummary> cache) {
        if (operatorId == null) {
            return null;
        }
        return cache.computeIfAbsent(operatorId, id ->
                operatorRepository.findById(id).map(OperatorSummary::from).orElse(null));
    }
}
