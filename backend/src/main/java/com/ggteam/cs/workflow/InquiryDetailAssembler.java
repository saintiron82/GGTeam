package com.ggteam.cs.workflow;

import com.ggteam.cs.common.BusinessException;
import com.ggteam.cs.common.ErrorCode;
import com.ggteam.cs.persistence.entity.AIAnalysis;
import com.ggteam.cs.persistence.entity.ApprovalHistory;
import com.ggteam.cs.persistence.entity.Diagnosis;
import com.ggteam.cs.persistence.entity.DraftResponse;
import com.ggteam.cs.persistence.entity.Inquiry;
import com.ggteam.cs.persistence.entity.Operator;
import com.ggteam.cs.persistence.repository.AIAnalysisRepository;
import com.ggteam.cs.persistence.repository.ApprovalHistoryRepository;
import com.ggteam.cs.persistence.repository.DiagnosisRepository;
import com.ggteam.cs.persistence.repository.DraftResponseRepository;
import com.ggteam.cs.persistence.repository.InquiryRepository;
import com.ggteam.cs.persistence.repository.OperatorRepository;
import com.ggteam.cs.workflow.dto.HistoryEntry;
import com.ggteam.cs.workflow.dto.InquiryDetailDto;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * InquiryDetail(01-api-contract §6) 조립기.
 *
 * <p>상세 조립의 정식 소유는 백엔드 A(US-23)이나, C의 Pull 배정 응답에 동일 구조가 필요하여
 * C 브랜치에서 stub 선행으로 구현한다. 머지 시 단일 구현으로 통합한다.
 */
@Component
public class InquiryDetailAssembler {

    private final InquiryRepository inquiryRepository;
    private final AIAnalysisRepository aiAnalysisRepository;
    private final DiagnosisRepository diagnosisRepository;
    private final DraftResponseRepository draftResponseRepository;
    private final ApprovalHistoryRepository approvalHistoryRepository;
    private final OperatorRepository operatorRepository;

    public InquiryDetailAssembler(InquiryRepository inquiryRepository,
                                  AIAnalysisRepository aiAnalysisRepository,
                                  DiagnosisRepository diagnosisRepository,
                                  DraftResponseRepository draftResponseRepository,
                                  ApprovalHistoryRepository approvalHistoryRepository,
                                  OperatorRepository operatorRepository) {
        this.inquiryRepository = inquiryRepository;
        this.aiAnalysisRepository = aiAnalysisRepository;
        this.diagnosisRepository = diagnosisRepository;
        this.draftResponseRepository = draftResponseRepository;
        this.approvalHistoryRepository = approvalHistoryRepository;
        this.operatorRepository = operatorRepository;
    }

    @Transactional(readOnly = true)
    public InquiryDetailDto assemble(UUID inquiryId) {
        Inquiry inquiry = inquiryRepository.findById(inquiryId)
                .orElseThrow(() -> new BusinessException(ErrorCode.INQUIRY_NOT_FOUND));

        InquiryDetailDto.InquiryPart inquiryPart = new InquiryDetailDto.InquiryPart(
                inquiry.getId(),
                inquiry.getCustomerInfo(),
                inquiry.getCustomerType().name(),
                inquiry.getContent(),
                inquiry.getStatus().name(),
                inquiry.getCreatedAt(),
                inquiry.getAssignedOperatorId());

        InquiryDetailDto.AnalysisPart analysisPart = aiAnalysisRepository.findByInquiryId(inquiryId)
                .map(this::toAnalysisPart).orElse(null);

        InquiryDetailDto.DiagnosisPart diagnosisPart = diagnosisRepository.findByInquiryId(inquiryId)
                .map(this::toDiagnosisPart).orElse(null);

        InquiryDetailDto.DraftPart draftPart = draftResponseRepository
                .findTopByInquiryIdOrderByCreatedAtDesc(inquiryId)
                .map(this::toDraftPart).orElse(null);

        List<HistoryEntry> history = toHistory(
                approvalHistoryRepository.findByInquiryIdOrderByTimestampAsc(inquiryId));

        return new InquiryDetailDto(inquiryPart, analysisPart, diagnosisPart, draftPart, history);
    }

    private InquiryDetailDto.AnalysisPart toAnalysisPart(AIAnalysis a) {
        return new InquiryDetailDto.AnalysisPart(
                a.getAiType() != null ? a.getAiType().name() : null,
                a.getSubCategory(),
                a.getUrgency() != null ? a.getUrgency().name() : null,
                a.getSummary(),
                a.getKeywords(),
                a.getSystemQueryResult(),
                a.getFailureType() != null ? a.getFailureType().name() : null);
    }

    private InquiryDetailDto.DiagnosisPart toDiagnosisPart(Diagnosis d) {
        return new InquiryDetailDto.DiagnosisPart(d.getCause(), d.getSuggestedDirection(), d.getConfidence());
    }

    private InquiryDetailDto.DraftPart toDraftPart(DraftResponse d) {
        return new InquiryDetailDto.DraftPart(d.getId(), d.getContent(),
                d.getStatus().name(), d.getRegenerationCount());
    }

    /** 타임라인 단독 조회 (US-18, US-21). */
    @Transactional(readOnly = true)
    public List<HistoryEntry> history(UUID inquiryId) {
        return toHistory(approvalHistoryRepository.findByInquiryIdOrderByTimestampAsc(inquiryId));
    }

    /** 이력 → 타임라인. operator 표시명을 배치 조회로 해석. */
    public List<HistoryEntry> toHistory(List<ApprovalHistory> histories) {
        List<UUID> operatorIds = histories.stream()
                .map(ApprovalHistory::getOperatorId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        Map<UUID, String> names = new HashMap<>();
        if (!operatorIds.isEmpty()) {
            names = operatorRepository.findByIdIn(operatorIds).stream()
                    .collect(Collectors.toMap(Operator::getId, Operator::getUsername));
        }
        Map<UUID, String> finalNames = names;
        return histories.stream()
                .map(h -> new HistoryEntry(
                        h.getAction().name(),
                        Optional.ofNullable(finalNames.get(h.getOperatorId()))
                                .orElse(String.valueOf(h.getOperatorId())),
                        h.getReason(),
                        h.getTimestamp()))
                .toList();
    }
}
