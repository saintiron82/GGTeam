package com.ggteam.cs.auth;

import com.ggteam.cs.auth.dto.LoginRequest;
import com.ggteam.cs.auth.dto.LoginResponse;
import com.ggteam.cs.common.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 인증 API (01-api-contract §1).
 *
 * <p>담당: 백엔드 A.
 */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * 로그인. 성공 시 JWT + 운영자 요약 + 만료시각 반환.
     * 에러: INVALID_CREDENTIALS(401), ACCOUNT_LOCKED(423) — GlobalExceptionHandler가 변환.
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.of(response));
    }

    /**
     * 로그아웃. JWT는 무상태이므로 서버 세션 제거 없이 성공 응답.
     * (클라이언트가 토큰을 폐기한다. 블랙리스트는 MVP 범위 외.)
     */
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Map<String, Boolean>>> logout() {
        return ResponseEntity.ok(ApiResponse.of(Map.of("success", true)));
    }
}
