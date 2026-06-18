package com.ggteam.cs.workflow.api;

import com.ggteam.cs.common.ApiResponse;
import com.ggteam.cs.workflow.ApprovalService;
import com.ggteam.cs.workflow.InquiryDetailAssembler;
import com.ggteam.cs.workflow.dto.HistoryEntry;
import com.ggteam.cs.workflow.dto.InquiryDetailDto;
import com.ggteam.cs.workflow.dto.OperatorRequests.ApproveRequest;
import com.ggteam.cs.workflow.dto.OperatorRequests.DraftEditRequest;
import com.ggteam.cs.workflow.dto.OperatorRequests.ReanalyzeRequest;
import com.ggteam.cs.workflow.dto.OperatorRequests.RejectRequest;
import com.ggteam.cs.workflow.dto.OperatorResponses.ApproveResponse;
import com.ggteam.cs.workflow.dto.OperatorResponses.DraftEditResponse;
import com.ggteam.cs.workflow.dto.OperatorResponses.NewDraft;
import com.ggteam.cs.workflow.dto.OperatorResponses.ReanalyzeResponse;
import com.ggteam.cs.workflow.dto.OperatorResponses.RejectResponse;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 운영자 워크플로우 API (01-api-contract §3). <b>담당: 백엔드 C.</b>
 *
 * <p>모든 엔드포인트는 인증 필요(BR-37). 현재 운영자는 {@link CurrentOperatorProvider}로 해석.
 */
@RestController
@RequestMapping("/api/v1/operator/inquiries")
public class OperatorController {

    private final ApprovalService approvalService;
    private final InquiryDetailAssembler detailAssembler;
    private final CurrentOperatorProvider currentOperator;

    public OperatorController(ApprovalService approvalService,
                              InquiryDetailAssembler detailAssembler,
                              CurrentOperatorProvider currentOperator) {
        this.approvalService = approvalService;
        this.detailAssembler = detailAssembler;
        this.currentOperator = currentOperator;
    }

    /** Pull 배정: 가용 문의 1건 배정 후 상세 반환. 없으면 204. */
    @PostMapping("/pull")
    public ResponseEntity<ApiResponse<InquiryDetailDto>> pull() {
        UUID operatorId = currentOperator.currentOperatorId();
        Optional<UUID> assigned = approvalService.pullAssign(operatorId);
        return assigned
                .map(id -> ResponseEntity.ok(ApiResponse.of(detailAssembler.assemble(id))))
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    /** 답변 초안 수정. */
    @PatchMapping("/{inquiryId}/draft")
    public ResponseEntity<ApiResponse<DraftEditResponse>> editDraft(
            @PathVariable UUID inquiryId,
            @Valid @RequestBody DraftEditRequest request) {
        UUID operatorId = currentOperator.currentOperatorId();
        DraftEditResponse result = approvalService.edit(inquiryId, operatorId, request.content());
        return ResponseEntity.ok(ApiResponse.of(result));
    }

    /** 승인 → 발송 트리거. content가 있으면 수정 후 승인. */
    @PostMapping("/{inquiryId}/approve")
    public ResponseEntity<ApiResponse<ApproveResponse>> approve(
            @PathVariable UUID inquiryId,
            @RequestBody(required = false) ApproveRequest request) {
        UUID operatorId = currentOperator.currentOperatorId();
        String editedContent = request != null ? request.content() : null;
        approvalService.approve(inquiryId, operatorId, editedContent);
        return ResponseEntity.ok(ApiResponse.of(new ApproveResponse(inquiryId, "APPROVED")));
    }

    /** 반려 → AI 재생성. */
    @PostMapping("/{inquiryId}/reject")
    public ResponseEntity<ApiResponse<RejectResponse>> reject(
            @PathVariable UUID inquiryId,
            @Valid @RequestBody RejectRequest request) {
        UUID operatorId = currentOperator.currentOperatorId();
        NewDraft newDraft = approvalService.reject(inquiryId, operatorId, request.reason());
        return ResponseEntity.ok(ApiResponse.of(new RejectResponse(inquiryId, newDraft)));
    }

    /** 운영자 수동 재분석 요청. */
    @PostMapping("/{inquiryId}/reanalyze")
    public ResponseEntity<ApiResponse<ReanalyzeResponse>> reanalyze(
            @PathVariable UUID inquiryId,
            @RequestBody(required = false) ReanalyzeRequest request) {
        UUID operatorId = currentOperator.currentOperatorId();
        String reason = request != null ? request.reason() : null;
        approvalService.reanalyze(inquiryId, operatorId, reason);
        return ResponseEntity.ok(ApiResponse.of(new ReanalyzeResponse(inquiryId, "AI_ANALYZING")));
    }

    /** 처리 이력 타임라인. */
    @GetMapping("/{inquiryId}/history")
    public ResponseEntity<ApiResponse<List<HistoryEntry>>> history(@PathVariable UUID inquiryId) {
        currentOperator.currentOperatorId(); // 인증 확인
        List<HistoryEntry> history = detailAssembler.history(inquiryId);
        return ResponseEntity.ok(ApiResponse.of(history));
    }
}
