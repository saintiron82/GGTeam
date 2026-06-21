package com.ggteam.cs.sim;

/** 계획된 문의 1건을 실제 접수 경로로 전송한다. */
public interface InquirySender {
    void send(PlannedInquiry inquiry);
}
