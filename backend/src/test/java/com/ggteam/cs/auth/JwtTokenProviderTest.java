package com.ggteam.cs.auth;

import com.ggteam.cs.common.enums.OperatorRole;
import com.ggteam.cs.persistence.entity.Operator;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * JwtTokenProvider 단위 테스트 — 토큰 발급/검증 왕복 (BR-34).
 */
@DisplayName("JwtTokenProvider 토큰 왕복")
class JwtTokenProviderTest {

    // HS256은 최소 256비트(32바이트) 키 필요
    private static final String SECRET = "test-secret-key-for-jwt-provider-unit-test-32bytes-or-more";

    private JwtTokenProvider provider;

    @BeforeEach
    void setUp() {
        provider = new JwtTokenProvider(SECRET, 8);
    }

    private Operator operator(UUID id, String username, OperatorRole role) {
        Operator op = new Operator();
        ReflectionTestUtils.setField(op, "id", id);
        op.setUsername(username);
        op.setRole(role);
        return op;
    }

    @Test
    @DisplayName("발급한 토큰을 파싱하면 동일한 principal 복원")
    void generateThenParse_roundTrip() {
        UUID id = UUID.randomUUID();
        Operator op = operator(id, "alice", OperatorRole.ADMIN);

        String token = provider.generateToken(op);
        AuthPrincipal principal = provider.parsePrincipal(token);

        assertThat(principal.operatorId()).isEqualTo(id);
        assertThat(principal.username()).isEqualTo("alice");
        assertThat(principal.role()).isEqualTo(OperatorRole.ADMIN);
    }

    @Test
    @DisplayName("만료시각은 발급 시점 + 8시간 근방")
    void resolveExpiry_about8Hours() {
        Operator op = operator(UUID.randomUUID(), "bob", OperatorRole.OPERATOR);
        String token = provider.generateToken(op);

        long epochSecExp = provider.resolveExpiry(token).toEpochSecond();
        long expected = System.currentTimeMillis() / 1000 + 8 * 3600;
        // 실행 지연 감안 ±60초 허용
        assertThat(epochSecExp).isBetween(expected - 60, expected + 60);
    }

    @Test
    @DisplayName("위조/잘못된 토큰 파싱 시 예외")
    void parse_invalidToken_throws() {
        assertThatThrownBy(() -> provider.parsePrincipal("not-a-valid-jwt"))
                .isInstanceOf(JwtException.class);
    }

    @Test
    @DisplayName("다른 시크릿으로 서명된 토큰은 검증 실패")
    void parse_tamperedSignature_throws() {
        Operator op = operator(UUID.randomUUID(), "carol", OperatorRole.OPERATOR);
        String token = new JwtTokenProvider("another-different-secret-key-32bytes-or-longer-xx", 8)
                .generateToken(op);

        assertThatThrownBy(() -> provider.parsePrincipal(token))
                .isInstanceOf(JwtException.class);
    }
}
