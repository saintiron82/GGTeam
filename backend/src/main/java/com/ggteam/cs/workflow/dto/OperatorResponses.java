package com.ggteam.cs.workflow.dto;

import java.util.UUID;

/**
 * 운영자 워크플로우 응답 DTO 모음 (01-api-contract §3).
 */
public final class OperatorResponses {

    private OperatorResponses() {}

    /** PATCH /draft 응답. */
    public record DraftEditResponse(UUID draftId, String content, String status) {}

    /** POST /approve 응답. */
    public record ApproveResponse(UUID inquiryId, String status) {}

    /** 반려 후 재생성된 새 초안 정보. */
    public record NewDraft(UUID draftId, String content, int regenerationCount) {}

    /** POST /reject 응답. */
    public record RejectResponse(UUID inquiryId, NewDraft newDraft) {}

    /** POST /reanalyze 응답. */
    public record ReanalyzeResponse(UUID inquiryId, String status) {}
}
