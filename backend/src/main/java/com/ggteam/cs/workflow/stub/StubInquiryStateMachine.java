package com.ggteam.cs.workflow.stub;

import com.ggteam.cs.common.BusinessException;
import com.ggteam.cs.common.ErrorCode;
import com.ggteam.cs.common.enums.ApprovalAction;
import com.ggteam.cs.common.enums.InquiryStatus;
import com.ggteam.cs.persistence.entity.Inquiry;
import com.ggteam.cs.persistence.repository.InquiryRepository;
import com.ggteam.cs.workflow.InquiryStateMachine;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * {@link InquiryStateMachine} stub 구현 — <b>백엔드 A 정식 구현 전 병렬 개발용.</b>
 *
 * <p>business-rules §2 전이 테이블(BR-07~10)을 충실히 구현하므로 백엔드 C의 워크플로우
 * (승인/반려/재분석) 통합 동작을 검증할 수 있다. A의 정식 빈이 등록되면
 * {@code @ConditionalOnMissingBean}에 의해 대체된다.
 */
public class StubInquiryStateMachine implements InquiryStateMachine {

    private static final Map<InquiryStatus, Set<InquiryStatus>> TABLE = buildTable();

    private final InquiryRepository inquiryRepository;

    public StubInquiryStateMachine(InquiryRepository inquiryRepository) {
        this.inquiryRepository = inquiryRepository;
    }

    private static Map<InquiryStatus, Set<InquiryStatus>> buildTable() {
        Map<InquiryStatus, Set<InquiryStatus>> t = new EnumMap<>(InquiryStatus.class);
        t.put(InquiryStatus.RECEIVED, EnumSet.of(InquiryStatus.AI_ANALYZING));
        t.put(InquiryStatus.AI_ANALYZING,
                EnumSet.of(InquiryStatus.PENDING_ASSIGNMENT, InquiryStatus.MANUAL_CLASSIFICATION_PENDING));
        t.put(InquiryStatus.PENDING_ASSIGNMENT,
                EnumSet.of(InquiryStatus.OPERATOR_REVIEWING, InquiryStatus.AI_ANALYZING));
        // OPERATOR_REVIEWING: 자기루프(수정/반려) + 승인 + 재분석 (BR-10)
        t.put(InquiryStatus.OPERATOR_REVIEWING,
                EnumSet.of(InquiryStatus.OPERATOR_REVIEWING, InquiryStatus.APPROVED, InquiryStatus.AI_ANALYZING));
        t.put(InquiryStatus.APPROVED, EnumSet.of(InquiryStatus.SENT));
        t.put(InquiryStatus.MANUAL_CLASSIFICATION_PENDING,
                EnumSet.of(InquiryStatus.OPERATOR_REVIEWING, InquiryStatus.PENDING_ASSIGNMENT,
                        InquiryStatus.AI_ANALYZING));
        t.put(InquiryStatus.SENT, EnumSet.noneOf(InquiryStatus.class)); // 종료 (BR-08)
        return t;
    }

    @Override
    public void transition(UUID inquiryId, InquiryStatus to, ApprovalAction action, UUID operatorId) {
        Inquiry inquiry = inquiryRepository.findById(inquiryId)
                .orElseThrow(() -> new BusinessException(ErrorCode.INQUIRY_NOT_FOUND));
        if (!canTransition(inquiry.getStatus(), to)) {
            throw new BusinessException(ErrorCode.INVALID_STATE_TRANSITION,
                    "전이 불가: %s -> %s".formatted(inquiry.getStatus(), to));
        }
        inquiry.setStatus(to);
        inquiryRepository.save(inquiry);
    }

    @Override
    public boolean canTransition(InquiryStatus from, InquiryStatus to) {
        return TABLE.getOrDefault(from, EnumSet.noneOf(InquiryStatus.class)).contains(to);
    }
}
