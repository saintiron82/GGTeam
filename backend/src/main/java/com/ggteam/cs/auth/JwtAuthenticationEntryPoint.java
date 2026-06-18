package com.ggteam.cs.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ggteam.cs.common.ErrorCode;
import com.ggteam.cs.common.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * 미인증 요청에 대한 표준 401 응답 (02-shared-contracts §4: UNAUTHORIZED).
 * 공통 ErrorResponse 포맷을 유지한다.
 *
 * <p>담당: 백엔드 A.
 */
@Component
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    public JwtAuthenticationEntryPoint(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException) throws IOException {

        ErrorResponse body = ErrorResponse.of(ErrorCode.UNAUTHORIZED, ErrorCode.UNAUTHORIZED.getDefaultMessage());
        response.setStatus(ErrorCode.UNAUTHORIZED.getStatus().value());
        response.setContentType("application/json");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        objectMapper.writeValue(response.getWriter(), body);
    }
}
