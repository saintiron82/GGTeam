package com.ggteam.cs.common;

import java.time.ZonedDateTime;

/**
 * 표준 성공 응답 래퍼 (01-api-contract §0).
 * { "data": ..., "timestamp": "...+09:00" }
 */
public record ApiResponse<T>(T data, ZonedDateTime timestamp) {

    public static <T> ApiResponse<T> of(T data) {
        return new ApiResponse<>(data, ZonedDateTime.now());
    }
}
