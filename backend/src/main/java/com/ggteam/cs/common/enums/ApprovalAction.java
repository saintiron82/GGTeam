package com.ggteam.cs.common.enums;

/** 운영자 처리 이력 액션 (ApprovalHistory.action). */
public enum ApprovalAction {
    APPROVE,     // 승인
    REJECT,      // 반려
    EDIT,        // 수정
    REGENERATE,  // 재생성
    ASSIGN,      // 배정
    REANALYZE    // 재분석 (운영자 수동 요청)
}
