package com.ggteam.cs.auth.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 로그인 요청 (01-api-contract §1 POST /auth/login).
 */
public record LoginRequest(
        @NotBlank(message = "username은 필수입니다.") String username,
        @NotBlank(message = "password는 필수입니다.") String password
) {}
