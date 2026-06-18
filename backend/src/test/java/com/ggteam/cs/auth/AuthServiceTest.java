package com.ggteam.cs.auth;

import com.ggteam.cs.auth.dto.LoginRequest;
import com.ggteam.cs.auth.dto.LoginResponse;
import com.ggteam.cs.common.BusinessException;
import com.ggteam.cs.common.ErrorCode;
import com.ggteam.cs.common.enums.OperatorRole;
import com.ggteam.cs.persistence.entity.Operator;
import com.ggteam.cs.persistence.repository.OperatorRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.ZonedDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * AuthService 단위 테스트 — 인증/계정 잠금 정책 (BR-31~38).
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService 로그인/계정잠금")
class AuthServiceTest {

    private static final int MAX_FAILED_LOGIN = 5;

    @Mock OperatorRepository operatorRepository;
    @Mock org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;
    @Mock JwtTokenProvider jwtTokenProvider;

    AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(operatorRepository, passwordEncoder, jwtTokenProvider, MAX_FAILED_LOGIN);
    }

    private Operator operator(String username, String hash, int failed, boolean locked) {
        Operator op = new Operator();
        op.setUsername(username);
        op.setPasswordHash(hash);
        op.setRole(OperatorRole.OPERATOR);
        op.setFailedLoginCount(failed);
        op.setLocked(locked);
        return op;
    }

    @Test
    @DisplayName("로그인 성공 시 토큰 발급 + 실패 카운트 0 초기화 (BR-33)")
    void login_success_resetsFailedCount() {
        Operator op = operator("alice", "hash", 3, false);
        when(operatorRepository.findByUsername("alice")).thenReturn(Optional.of(op));
        when(passwordEncoder.matches("pw", "hash")).thenReturn(true);
        when(jwtTokenProvider.generateToken(op)).thenReturn("jwt-token");
        ZonedDateTime exp = ZonedDateTime.now().plusHours(8);
        when(jwtTokenProvider.resolveExpiry("jwt-token")).thenReturn(exp);

        LoginResponse res = authService.login(new LoginRequest("alice", "pw"));

        assertThat(res.token()).isEqualTo("jwt-token");
        assertThat(res.operator().username()).isEqualTo("alice");
        assertThat(res.expiresAt()).isEqualTo(exp);
        assertThat(op.getFailedLoginCount()).isZero();
        verify(operatorRepository).save(op);
    }

    @Test
    @DisplayName("존재하지 않는 사용자 → INVALID_CREDENTIALS")
    void login_unknownUser_throwsInvalidCredentials() {
        when(operatorRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(new LoginRequest("ghost", "pw")))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_CREDENTIALS);
    }

    @Test
    @DisplayName("비밀번호 불일치 → 실패 카운트 증가 + INVALID_CREDENTIALS")
    void login_wrongPassword_incrementsFailure() {
        Operator op = operator("bob", "hash", 1, false);
        when(operatorRepository.findByUsername("bob")).thenReturn(Optional.of(op));
        when(passwordEncoder.matches("bad", "hash")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(new LoginRequest("bob", "bad")))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_CREDENTIALS);

        assertThat(op.getFailedLoginCount()).isEqualTo(2);
        assertThat(op.isLocked()).isFalse();
        verify(operatorRepository).save(op);
    }

    @Test
    @DisplayName("연속 실패 5회 도달 시 계정 잠금 (BR-31)")
    void login_fifthFailure_locksAccount() {
        Operator op = operator("carol", "hash", MAX_FAILED_LOGIN - 1, false);
        when(operatorRepository.findByUsername("carol")).thenReturn(Optional.of(op));
        when(passwordEncoder.matches("bad", "hash")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(new LoginRequest("carol", "bad")))
                .isInstanceOf(BusinessException.class);

        assertThat(op.getFailedLoginCount()).isEqualTo(MAX_FAILED_LOGIN);
        assertThat(op.isLocked()).isTrue();
        verify(operatorRepository).save(op);
    }

    @Test
    @DisplayName("잠긴 계정은 비밀번호와 무관하게 ACCOUNT_LOCKED (BR-32)")
    void login_lockedAccount_throwsAccountLocked() {
        Operator op = operator("dave", "hash", MAX_FAILED_LOGIN, true);
        when(operatorRepository.findByUsername("dave")).thenReturn(Optional.of(op));

        assertThatThrownBy(() -> authService.login(new LoginRequest("dave", "pw")))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.ACCOUNT_LOCKED);

        // 잠긴 계정은 비밀번호 검증 자체를 하지 않음
        verify(passwordEncoder, never()).matches(anyString(), anyString());
        verify(jwtTokenProvider, never()).generateToken(any());
    }
}
