package com.ggteam.cs.inquiry.dto;

import com.ggteam.cs.common.enums.InquiryStatus;
import com.ggteam.cs.persistence.entity.Inquiry;

import java.time.ZonedDateTime;
import java.util.UUID;

/**
 * 문의 접수 응답 (01-api-contract §2).
 * { "inquiryId": "uuid", "status": "RECEIVED", "createdAt": "...+09:00" }
 */
public record CreateInquiryResponse(UUID inquiryId, InquiryStatus status, ZonedDateTime createdAt) {

    public static CreateInquiryResponse from(Inquiry inquiry) {
        return new CreateInquiryResponse(inquiry.getId(), inquiry.getStatus(), inquiry.getCreatedAt());
    }
}
