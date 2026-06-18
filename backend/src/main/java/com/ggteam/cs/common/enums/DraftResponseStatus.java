package com.ggteam.cs.common.enums;

/** 답변 초안 상태. */
public enum DraftResponseStatus {
    GENERATED,  // 생성됨
    EDITED,     // 수정됨
    REJECTED,   // 반려됨 (재생성 대상)
    APPROVED    // 승인됨 (발송 대상)
}
