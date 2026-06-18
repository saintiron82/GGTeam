package com.ggteam.cs.common.enums;

/**
 * 문의 처리 상태 (02-shared-contracts §1, business-rules §2).
 * 전이는 InquiryStateMachine을 통해서만 수행한다 (BR-09).
 */
public enum InquiryStatus {
    RECEIVED,                        // 접수
    AI_ANALYZING,                    // AI분석중
    PENDING_ASSIGNMENT,              // 담당자배정대기
    OPERATOR_REVIEWING,              // 운영자확인중
    APPROVED,                        // 승인완료
    SENT,                            // 발송완료 (종료)
    MANUAL_CLASSIFICATION_PENDING    // 수동분류대기 (AI 실패 등)
}
