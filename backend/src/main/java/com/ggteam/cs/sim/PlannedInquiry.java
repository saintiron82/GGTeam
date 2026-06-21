package com.ggteam.cs.sim;

import com.ggteam.cs.common.enums.InquiryType;

/** 드립으로 주입할 사전 작성 문의 1건. */
public record PlannedInquiry(String userId, InquiryType type, String content, SimScenario scenario) {}
