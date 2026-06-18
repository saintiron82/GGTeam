package com.ggteam.cs.workflow.state;

import com.ggteam.cs.common.BusinessException;
import com.ggteam.cs.common.ErrorCode;
import com.ggteam.cs.common.enums.ApprovalAction;
import com.ggteam.cs.common.enums.InquiryStatus;
import com.ggteam.cs.persistence.entity.Inquiry;
import com.ggteam.cs.persistence.repository.InquiryRepository;
import com.ggteam.cs.workflow.InquiryStateMachine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * 문의 상태 전이 단일 관리자 구현 (BR-07~10).
 *
 * <p>전이 테이블(business-rules §2.1)에 정의된 전이만 허용한다. 정의되지 않은 전이는
 * {@link ErrorCode#INVALID_STATE_TRANSITION}(409)으로 거부한다(BR-07). {@code SENT}는
 * 종료 상태로 어떤 전이도 불가하다(BR-08).
 *
 * <p>책임 범위: <b>전이 검증 + 상태 적용</b>만 담당한다. ApprovalHistory 기록은 운영자 액션
 * 주체(ApprovalService, 백엔드 C)와 시스템 단계(AIAnalysisService, 백엔드 B)가 수행한다.
 * 본 컴포넌트는 {@code action}/{@code operatorId}를 감사 로깅 컨텍스트로만 사용한다.
 *
 * <p>담당: 백엔드 A.
 */
@Component
public class DefaultInquiryStateMachine implements InquiryStateMachine {

    private static final Logger log = LoggerFactory.getLogger(DefaultInquiryStateMachine.class);

    /** 전이 테이블 (business-rules §2.1). key=현재 상태, value=허용되는 다음 상태 집합. */
    private static final Map<InquiryStatus, Set<InquiryStatus>> TRANSITIONS =
            new EnumMap<>(InquiryStatus.class);

    static {
        TRANSITIONS.put(InquiryStatus.RECEIVED,
                Set.of(InquiryStatus.AI_ANALYZING));
        TRANSITIONS.put(InquiryStatus.AI_ANALYZING,
                Set.of(InquiryStatus.PENDING_ASSIGNMENT, InquiryStatus.MANUAL_CLASSIFICATION_PENDING));
        TRANSITIONS.put(InquiryStatus.PENDING_ASSIGNMENT,
                Set.of(InquiryStatus.OPERATOR_REVIEWING, InquiryStatus.AI_ANALYZING));
        // OPERATOR_REVIEWING: 수정/반려는 자기루프(BR-10), 승인→APPROVED, 재분석→AI_ANALYZING
        TRANSITIONS.put(InquiryStatus.OPERATOR_REVIEWING,
                Set.of(InquiryStatus.OPERATOR_REVIEWING, InquiryStatus.APPROVED, InquiryStatus.AI_ANALYZING));
        TRANSITIONS.put(InquiryStatus.APPROVED,
                Set.of(InquiryStatus.SENT));
        TRANSITIONS.put(InquiryStatus.MANUAL_CLASSIFICATION_PENDING,
                Set.of(InquiryStatus.OPERATOR_REVIEWING, InquiryStatus.PENDING_ASSIGNMENT, InquiryStatus.AI_ANALYZING));
        // SENT: 종료 상태 — 전이 없음 (BR-08)
        TRANSITIONS.put(InquiryStatus.SENT, Set.of());
    }

    private final InquiryRepository inquiryRepository;

    public DefaultInquiryStateMachine(InquiryRepository inquiryRepository) {
        this.inquiryRepository = inquiryRepository;
    }

    @Override
    @Transactional
    public void transition(UUID inquiryId, InquiryStatus to, ApprovalAction action, UUID operatorId) {
        Inquiry inquiry = inquiryRepository.findById(inquiryId)
                .orElseThrow(() -> new BusinessException(ErrorCode.INQUIRY_NOT_FOUND));

        InquiryStatus from = inquiry.getStatus();
        if (!canTransition(from, to)) {
            throw new BusinessException(
                    ErrorCode.INVALID_STATE_TRANSITION,
                    "허용되지 않은 상태 전이입니다: " + from + " -> " + to);
        }

        inquiry.setStatus(to);
        inquiryRepository.save(inquiry);

        log.info("Inquiry {} 상태 전이 {} -> {} (action={}, operatorId={})",
                inquiryId, from, to, action, operatorId);
    }

    @Override
    public boolean canTransition(InquiryStatus from, InquiryStatus to) {
        if (from == null || to == null) {
            return false;
        }
        return TRANSITIONS.getOrDefault(from, Set.of()).contains(to);
    }
}
