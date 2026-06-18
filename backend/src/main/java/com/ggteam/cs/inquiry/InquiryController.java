package com.ggteam.cs.inquiry;

import com.ggteam.cs.common.ApiResponse;
import com.ggteam.cs.inquiry.dto.CreateInquiryRequest;
import com.ggteam.cs.inquiry.dto.CreateInquiryResponse;
import com.ggteam.cs.inquiry.dto.InquiryDetail;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * 문의 API (01-api-contract §2).
 *
 * <ul>
 *   <li>POST /api/v1/inquiries — 고객 접수 (비인증 허용, SecurityConfig permitAll)</li>
 *   <li>GET /api/v1/inquiries/{inquiryId} — 상세 조회 (인증 필요)</li>
 * </ul>
 *
 * <p>담당: 백엔드 A.
 */
@RestController
@RequestMapping("/api/v1/inquiries")
public class InquiryController {

    private final InquiryService inquiryService;

    public InquiryController(InquiryService inquiryService) {
        this.inquiryService = inquiryService;
    }

    /** 고객 문의 접수. 201 Created. 검증 실패 시 VALIDATION_ERROR(400). */
    @PostMapping
    public ResponseEntity<ApiResponse<CreateInquiryResponse>> create(
            @Valid @RequestBody CreateInquiryRequest request) {
        CreateInquiryResponse response = inquiryService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.of(response));
    }

    /** 문의 상세 조회. 없으면 INQUIRY_NOT_FOUND(404). */
    @GetMapping("/{inquiryId}")
    public ResponseEntity<ApiResponse<InquiryDetail>> getDetail(@PathVariable UUID inquiryId) {
        InquiryDetail detail = inquiryService.getDetail(inquiryId);
        return ResponseEntity.ok(ApiResponse.of(detail));
    }
}
