package com.ggteam.cs.workflow;

import com.ggteam.cs.aipipeline.AIAnalysisService;
import com.ggteam.cs.aipipeline.DraftResponseService;
import com.ggteam.cs.common.BusinessException;
import com.ggteam.cs.common.ErrorCode;
import com.ggteam.cs.common.enums.ApprovalAction;
import com.ggteam.cs.common.enums.DraftResponseStatus;
import com.ggteam.cs.common.enums.InquiryStatus;
import com.ggteam.cs.notification.NotificationService;
import com.ggteam.cs.persistence.entity.ApprovalHistory;
import com.ggteam.cs.persistence.entity.DraftResponse;
import com.ggteam.cs.persistence.entity.Inquiry;
import com.ggteam.cs.persistence.repository.ApprovalHistoryRepository;
import com.ggteam.cs.persistence.repository.DraftResponseRepository;
import com.ggteam.cs.persistence.repository.InquiryRepository;
import com.ggteam.cs.workflow.dto.OperatorResponses.DraftEditResponse;
import com.ggteam.cs.workflow.dto.OperatorResponses.NewDraft;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * 운영자 워크플로우 구현 (US-15~18, US-24). <b>담당: 백엔드 C.</b>
 *
 * <p>모든 상태 전이는 {@link InquiryStateMachine}을 통해서만 수행하고(BR-09),
 * 모든 운영자 액션은 {@link ApprovalHistory}에 append-only 기록한다(BR-22).
 */
@Service
public class ApprovalServiceImpl implements ApprovalService {

    private final InquiryRepository inquiryRepository;
    private final DraftResponseRepository draftResponseRepository;
    private final ApprovalHistoryRepository approvalHistoryRepository;
    private final InquiryStateMachine stateMachine;
    private final DraftResponseService draftResponseService;
    private final AIAnalysisService aiAnalysisService;
    private final NotificationService notificationService;

    public ApprovalServiceImpl(InquiryRepository inquiryRepository,
                               DraftResponseRepository draftResponseRepository,
                               ApprovalHistoryRepository approvalHistoryRepository,
                               InquiryStateMachine stateMachine,
                               DraftResponseService draftResponseService,
                               AIAnalysisService aiAnalysisService,
                               NotificationService notificationService) {
        this.inquiryRepository = inquiryRepository;
        this.draftResponseRepository = draftResponseRepository;
        this.approvalHistoryRepository = approvalHistoryRepository;
        this.stateMachine = stateMachine;
        this.draftResponseService = draftResponseService;
        this.aiAnalysisService = aiAnalysisService;
        this.notificationService = notificationService;
    }

    /**
     * Pull 배정 (BR-11~15). 우선순위(긴급도→FIFO) 후보를 순회하며 원자적 조건부 갱신으로
     * 선점한다. 동시 배정 충돌(0행 갱신) 시 다음 후보로 넘어간다.
     */
    @Override
    @Transactional
    public Optional<UUID> pullAssign(UUID operatorId) {
        List<Inquiry> candidates = inquiryRepository.findAssignableCandidates(PageRequest.of(0, 10));
        for (Inquiry candidate : candidates) {
            int updated = inquiryRepository.claimAssignment(candidate.getId(), operatorId);
            if (updated == 1) {
                // 배정 + OPERATOR_REVIEWING 전이가 원자적으로 완료됨(claimAssignment). 이력 기록(BR-15).
                approvalHistoryRepository.save(
                        ApprovalHistory.record(candidate.getId(), ApprovalAction.ASSIGN, operatorId, null));
                return Optional.of(candidate.getId());
            }
        }
        return Optional.empty(); // 가용 문의 없음 → 204
    }

    /**
     * 승인 (US-16, US-24). editedContent가 있으면 수정 후 승인. 활성 초안 필수(BR-21).
     * 승인 후 발송 트리거 (BR-23: 운영자 승인 거친 답변만 발송).
     */
    @Override
    @Transactional
    public void approve(UUID inquiryId, UUID operatorId, String editedContent) {
        Inquiry inquiry = loadInquiry(inquiryId);
        DraftResponse current = draftResponseRepository
                .findTopByInquiryIdOrderByCreatedAtDesc(inquiryId)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_STATE_TRANSITION,
                        "승인할 활성 초안이 없습니다."));

        // 수정 후 승인 (BR-20: EDIT는 regenerationCount 증가 안 함)
        if (StringUtils.hasText(editedContent)) {
            current.setContent(editedContent);
            current.setStatus(DraftResponseStatus.EDITED);
            draftResponseRepository.save(current);
            approvalHistoryRepository.save(
                    ApprovalHistory.record(inquiryId, ApprovalAction.EDIT, operatorId, null));
        }

        // 활성 초안 확정
        current.setStatus(DraftResponseStatus.APPROVED);
        draftResponseRepository.save(current);

        // 상태 전이 OPERATOR_REVIEWING -> APPROVED (BR-09)
        stateMachine.transition(inquiryId, InquiryStatus.APPROVED, ApprovalAction.APPROVE, operatorId);
        approvalHistoryRepository.save(
                ApprovalHistory.record(inquiryId, ApprovalAction.APPROVE, operatorId, null));

        // 자동 발송 트리거 (APPROVED -> SENT)
        notificationService.send(inquiryId);
    }

    /**
     * 수정만 (US-14, PATCH /draft). 상태 전이 없이 초안 내용 갱신, EDIT 이력 기록.
     * 반환: 수정된 활성 초안 정보.
     */
    @Override
    @Transactional
    public DraftEditResponse edit(UUID inquiryId, UUID operatorId, String content) {
        loadInquiry(inquiryId);
        DraftResponse current = draftResponseRepository
                .findTopByInquiryIdOrderByCreatedAtDesc(inquiryId)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_STATE_TRANSITION,
                        "수정할 활성 초안이 없습니다."));
        current.setContent(content);
        current.setStatus(DraftResponseStatus.EDITED);
        DraftResponse saved = draftResponseRepository.save(current);
        approvalHistoryRepository.save(
                ApprovalHistory.record(inquiryId, ApprovalAction.EDIT, operatorId, null));
        return new DraftEditResponse(saved.getId(), saved.getContent(), saved.getStatus().name());
    }

    /**
     * 반려 → 재생성 (US-17, BR-16~19). 사유 필수. REJECT 이력 후 재생성 위임(REGENERATE 이력).
     * 자기루프(상태 유지). 새 초안 정보 반환.
     */
    @Override
    @Transactional
    public NewDraft reject(UUID inquiryId, UUID operatorId, String reason) {
        if (!StringUtils.hasText(reason)) {
            throw new BusinessException(ErrorCode.REASON_REQUIRED);
        }
        loadInquiry(inquiryId);

        approvalHistoryRepository.save(
                ApprovalHistory.record(inquiryId, ApprovalAction.REJECT, operatorId, reason));

        // 재생성 위임 (백엔드 B). 한도 초과 시 REGENERATION_LIMIT_EXCEEDED 전파.
        UUID newDraftId = draftResponseService.regenerate(inquiryId, reason);

        approvalHistoryRepository.save(
                ApprovalHistory.record(inquiryId, ApprovalAction.REGENERATE, operatorId, reason));

        DraftResponse newDraft = draftResponseRepository.findById(newDraftId)
                .orElseThrow(() -> new BusinessException(ErrorCode.INTERNAL_ERROR, "재생성 초안 조회 실패"));
        return new NewDraft(newDraft.getId(), newDraft.getContent(), newDraft.getRegenerationCount());
    }

    /**
     * 운영자 수동 재분석 (US-24, BR-30a~c). AI_ANALYZING 전이 후 재분석 트리거.
     */
    @Override
    @Transactional
    public void reanalyze(UUID inquiryId, UUID operatorId, String reason) {
        loadInquiry(inquiryId);
        stateMachine.transition(inquiryId, InquiryStatus.AI_ANALYZING, ApprovalAction.REANALYZE, operatorId);
        approvalHistoryRepository.save(
                ApprovalHistory.record(inquiryId, ApprovalAction.REANALYZE, operatorId, reason));
        aiAnalysisService.analyze(inquiryId);
    }

    private Inquiry loadInquiry(UUID inquiryId) {
        return inquiryRepository.findById(inquiryId)
                .orElseThrow(() -> new BusinessException(ErrorCode.INQUIRY_NOT_FOUND));
    }
}
