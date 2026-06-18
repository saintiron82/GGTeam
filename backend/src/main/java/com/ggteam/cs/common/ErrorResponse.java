package com.ggteam.cs.common;

import java.time.ZonedDateTime;

/**
 * 표준 에러 응답 (01-api-contract §0).
 * { "error": { "code": "...", "message": "..." }, "timestamp": "...+09:00" }
 */
public record ErrorResponse(ErrorBody error, ZonedDateTime timestamp) {

    public record ErrorBody(String code, String message) {}

    public static ErrorResponse of(ErrorCode code, String message) {
        return new ErrorResponse(new ErrorBody(code.name(), message), ZonedDateTime.now());
    }
}
