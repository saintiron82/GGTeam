package com.ggteam.cs.workflow.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ggteam.cs.common.BusinessException;
import com.ggteam.cs.common.ErrorCode;
import com.ggteam.cs.common.GlobalExceptionHandler;
import com.ggteam.cs.workflow.ApprovalService;
import com.ggteam.cs.workflow.InquiryDetailAssembler;
import com.ggteam.cs.workflow.dto.OperatorResponses.DraftEditResponse;
import com.ggteam.cs.workflow.dto.OperatorResponses.NewDraft;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/**
 * OperatorController 통합 테스트 (MockMvc). API 계약(01-api-contract §3) 검증.
 * 보안 필터는 비활성화하고 운영자 식별은 CurrentOperatorProvider mock으로 대체.
 */
@WebMvcTest(controllers = OperatorController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class OperatorControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockBean ApprovalService approvalService;
    @MockBean InquiryDetailAssembler detailAssembler;
    @MockBean CurrentOperatorProvider currentOperator;

    private UUID operatorId;
    private UUID inquiryId;

    @BeforeEach
    void setUp() {
        operatorId = UUID.randomUUID();
        inquiryId = UUID.randomUUID();
        when(currentOperator.currentOperatorId()).thenReturn(operatorId);
    }

    @Test
    @DisplayName("POST /pull: 배정 가능 문의 없으면 204")
    void pull_noCandidate_returns204() throws Exception {
        when(approvalService.pullAssign(operatorId)).thenReturn(Optional.empty());

        mockMvc.perform(post("/api/v1/operator/inquiries/pull"))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("PATCH /draft: 초안 수정 200")
    void editDraft_returns200() throws Exception {
        UUID draftId = UUID.randomUUID();
        when(approvalService.edit(eq(inquiryId), eq(operatorId), any()))
                .thenReturn(new DraftEditResponse(draftId, "수정된 내용", "EDITED"));

        mockMvc.perform(patch("/api/v1/operator/inquiries/{id}/draft", inquiryId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("content", "수정된 내용"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("EDITED"));
    }

    @Test
    @DisplayName("POST /approve: 승인 200, status=APPROVED")
    void approve_returns200() throws Exception {
        mockMvc.perform(post("/api/v1/operator/inquiries/{id}/approve", inquiryId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("APPROVED"));

        verify(approvalService).approve(eq(inquiryId), eq(operatorId), any());
    }

    @Test
    @DisplayName("POST /reject: 사유 누락 시 400 VALIDATION_ERROR (BR-16)")
    void reject_blankReason_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/operator/inquiries/{id}/reject", inquiryId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("reason", ""))))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /reject: 정상 반려 시 새 초안 반환")
    void reject_returnsNewDraft() throws Exception {
        UUID newDraftId = UUID.randomUUID();
        when(approvalService.reject(eq(inquiryId), eq(operatorId), any()))
                .thenReturn(new NewDraft(newDraftId, "재생성 초안", 1));

        mockMvc.perform(post("/api/v1/operator/inquiries/{id}/reject", inquiryId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("reason", "톤이 부적절합니다"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.newDraft.regenerationCount").value(1));
    }

    @Test
    @DisplayName("POST /reject: 재생성 한도 초과 시 409")
    void reject_regenerationLimit_returns409() throws Exception {
        when(approvalService.reject(eq(inquiryId), eq(operatorId), any()))
                .thenThrow(new BusinessException(ErrorCode.REGENERATION_LIMIT_EXCEEDED));

        mockMvc.perform(post("/api/v1/operator/inquiries/{id}/reject", inquiryId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("reason", "다시"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("REGENERATION_LIMIT_EXCEEDED"));
    }

    @Test
    @DisplayName("POST /reanalyze: 재분석 200, status=AI_ANALYZING")
    void reanalyze_returns200() throws Exception {
        mockMvc.perform(post("/api/v1/operator/inquiries/{id}/reanalyze", inquiryId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("AI_ANALYZING"));

        verify(approvalService).reanalyze(eq(inquiryId), eq(operatorId), any());
    }

    @Test
    @DisplayName("GET /history: 이력 타임라인 200")
    void history_returns200() throws Exception {
        when(detailAssembler.history(inquiryId)).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/operator/inquiries/{id}/history", inquiryId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());
    }
}
