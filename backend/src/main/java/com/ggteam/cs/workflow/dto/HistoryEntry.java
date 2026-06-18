package com.ggteam.cs.workflow.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.ZonedDateTime;

/**
 * 처리 이력 타임라인 항목 (01-api-contract §3 history, §6).
 * operator는 운영자 표시명(username), 없으면 operatorId 문자열.
 */
@JsonInclude(JsonInclude.Include.ALWAYS)
public record HistoryEntry(
        String action,
        String operator,
        String reason,
        ZonedDateTime timestamp) {}
