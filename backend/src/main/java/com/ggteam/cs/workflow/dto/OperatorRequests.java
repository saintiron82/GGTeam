package com.ggteam.cs.workflow.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 운영자 워크플로우 요청 DTO 모음 (01-api-contract §3).
 */
public final class OperatorRequests {

    private OperatorRequests() {}

    /** PATCH /draft — 초안 수정. */
    public record DraftEditRequest(@NotBlank(message = "수정 내용은 비어 있을 수 없습니다.") String content) {}

    /** POST /approve — content는 선택(수정후승인). */
    public record ApproveRequest(String content) {}

    /** POST /reject — 사유 필수 (BR-16). */
    public record RejectRequest(@NotBlank(message = "반려 사유는 필수입니다.") String reason) {}

    /** POST /reanalyze — 사유 선택. */
    public record ReanalyzeRequest(String reason) {}
}
