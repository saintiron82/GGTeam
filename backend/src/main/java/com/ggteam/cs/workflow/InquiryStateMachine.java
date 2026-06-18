package com.ggteam.cs.workflow;

import com.ggteam.cs.common.enums.ApprovalAction;
import com.ggteam.cs.common.enums.InquiryStatus;

import java.util.UUID;

/**
 * 문의 상태 전이 단일 관리자 (BR-07~10). 모든 전이는 이 컴포넌트를 통해서만 수행.
 *
 * <p>담당: 백엔드 A. 가장 먼저 정의·커밋하여 B/C가 의존.
 */
public interface InquiryStateMachine {

    /** 전이 실행. 비정상 전이 시 BusinessException(INVALID_STATE_TRANSITION). */
    void transition(UUID inquiryId, InquiryStatus to, ApprovalAction action, UUID operatorId);

    /** 전이 가능 여부 검증 (전이 테이블 기반). */
    boolean canTransition(InquiryStatus from, InquiryStatus to);
}
