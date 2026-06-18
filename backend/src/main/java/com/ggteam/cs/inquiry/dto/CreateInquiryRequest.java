package com.ggteam.cs.inquiry.dto;

import com.ggteam.cs.common.enums.InquiryType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.Map;

/**
 * 고객 문의 접수 요청 (01-api-contract §2 POST /inquiries).
 *
 * <p>BR-01: content 최소 10자. BR-02: customerType은 정의된 enum. BR-03: customerInfo에 userId 포함
 * (userId 존재 검증은 서비스에서 수행).
 *
 * @param customerInfo 고객 식별 정보 (userId, nickname, channel 등)
 * @param customerType 문의 유형
 * @param content      문의 본문 (최소 10자)
 */
public record CreateInquiryRequest(
        @NotNull(message = "customerInfo는 필수입니다.") Map<String, Object> customerInfo,
        @NotNull(message = "customerType은 필수입니다.") InquiryType customerType,
        @NotBlank(message = "content는 필수입니다.")
        @Size(min = 10, message = "content는 최소 10자 이상이어야 합니다.") String content
) {}
