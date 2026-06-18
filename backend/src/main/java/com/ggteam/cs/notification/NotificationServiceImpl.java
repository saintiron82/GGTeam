package com.ggteam.cs.notification;

import com.ggteam.cs.common.BusinessException;
import com.ggteam.cs.common.ErrorCode;
import com.ggteam.cs.common.enums.ApprovalAction;
import com.ggteam.cs.common.enums.InquiryStatus;
import com.ggteam.cs.persistence.entity.DraftResponse;
import com.ggteam.cs.persistence.entity.Inquiry;
import com.ggteam.cs.persistence.repository.DraftResponseRepository;
import com.ggteam.cs.persistence.repository.InquiryRepository;
import com.ggteam.cs.workflow.InquiryStateMachine;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 발송 및 알림 구현 (US-25, BR-23~25b). <b>담당: 백엔드 C.</b>
 *
 * <p>승인(APPROVED)된 답변만 발송하고(BR-23, BR-24), 발송 성공 시 SENT로 전이한다(BR-25).
 * 발송 실패 시 재시도하며 시도 이력을 기록한다(BR-25a). 재시도 한도 초과 시 발송 실패로
 * 처리하고 운영자에게 노출할 수 있도록 예외를 전파한다.
 */
@Service
public class NotificationServiceImpl implements NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationServiceImpl.class);

    private final InquiryRepository inquiryRepository;
    private final DraftResponseRepository draftResponseRepository;
    private final InquiryStateMachine stateMachine;
    private final NotificationChannel channel;
    private final int maxRetries;

    public NotificationServiceImpl(InquiryRepository inquiryRepository,
                                   DraftResponseRepository draftResponseRepository,
                                   InquiryStateMachine stateMachine,
                                   NotificationChannel channel,
                                   @Value("${app.notification.max-retries:3}") int maxRetries) {
        this.inquiryRepository = inquiryRepository;
        this.draftResponseRepository = draftResponseRepository;
        this.stateMachine = stateMachine;
        this.channel = channel;
        this.maxRetries = maxRetries;
    }

    @Override
    @Transactional
    public void send(UUID inquiryId) {
        Inquiry inquiry = inquiryRepository.findById(inquiryId)
                .orElseThrow(() -> new BusinessException(ErrorCode.INQUIRY_NOT_FOUND));

        // BR-24: APPROVED 상태에서만 발송 가능
        if (inquiry.getStatus() != InquiryStatus.APPROVED) {
            throw new BusinessException(ErrorCode.INVALID_STATE_TRANSITION,
                    "발송은 APPROVED 상태에서만 가능합니다. 현재: " + inquiry.getStatus());
        }

        DraftResponse draft = draftResponseRepository
                .findTopByInquiryIdOrderByCreatedAtDesc(inquiryId)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_STATE_TRANSITION,
                        "발송할 답변 초안이 없습니다."));

        dispatchWithRetry(inquiryId, draft.getContent());

        // BR-25: 발송 성공 → SENT 전이
        stateMachine.transition(inquiryId, InquiryStatus.SENT, ApprovalAction.APPROVE, inquiry.getAssignedOperatorId());
        log.info("[notification] 발송 완료 inquiryId={} → SENT", inquiryId);
    }

    /** 재시도 루프 (BR-25a). 각 시도 결과/횟수/시각을 로깅. 한도 초과 시 예외 전파. */
    private void dispatchWithRetry(UUID inquiryId, String content) {
        int attempt = 0;
        RuntimeException lastError = null;
        while (attempt < maxRetries) {
            attempt++;
            try {
                channel.dispatch(inquiryId, content);
                log.info("[notification] 발송 시도 {}/{} 성공 inquiryId={}", attempt, maxRetries, inquiryId);
                return;
            } catch (RuntimeException ex) {
                lastError = ex;
                log.warn("[notification] 발송 시도 {}/{} 실패 inquiryId={} cause={}",
                        attempt, maxRetries, inquiryId, ex.getMessage());
            }
        }
        log.error("[notification] 발송 재시도 한도({}) 초과 inquiryId={}. 운영자 알림 필요.",
                maxRetries, inquiryId);
        throw new BusinessException(ErrorCode.INTERNAL_ERROR,
                "답변 발송에 실패했습니다(재시도 한도 초과). inquiryId=" + inquiryId
                        + (lastError != null ? ", cause=" + lastError.getMessage() : ""));
    }
}
