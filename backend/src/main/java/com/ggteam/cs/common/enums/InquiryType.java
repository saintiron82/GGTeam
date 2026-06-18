package com.ggteam.cs.common.enums;

/**
 * 문의 유형. MVP는 PAYMENT만 end-to-end 지원, 나머지는 확장 예정.
 */
public enum InquiryType {
    PAYMENT,        // 결제 (MVP)
    ITEM_DELIVERY,  // 아이템지급
    ACCOUNT,        // 계정
    ETC             // 기타
}
