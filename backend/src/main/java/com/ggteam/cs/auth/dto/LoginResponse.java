package com.ggteam.cs.auth.dto;

import java.time.ZonedDateTime;

/**
 * 로그인 응답 (01-api-contract §1).
 * { "token": "jwt...", "operator": { id, username, role }, "expiresAt": "...+09:00" }
 */
public record LoginResponse(String token, OperatorSummary operator, ZonedDateTime expiresAt) {}
