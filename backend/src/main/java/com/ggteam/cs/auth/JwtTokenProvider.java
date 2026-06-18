package com.ggteam.cs.auth;

import com.ggteam.cs.common.enums.OperatorRole;
import com.ggteam.cs.persistence.entity.Operator;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.UUID;

/**
 * JWT 발급/검증 (02-shared-contracts §4, BR-34/35).
 *
 * <p>Claims: sub(operatorId), username, role, iat, exp. 서명 HS256.
 * 유효기간은 발급 시점부터 {@code app.jwt.expiration-hours} (기본 8시간).
 *
 * <p>담당: 백엔드 A.
 */
@Component
public class JwtTokenProvider {

    private static final String CLAIM_USERNAME = "username";
    private static final String CLAIM_ROLE = "role";

    private final SecretKey key;
    private final long expirationSeconds;

    public JwtTokenProvider(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.expiration-hours:8}") long expirationHours) {
        // HS256은 최소 256비트(32바이트) 키 필요. 짧은 시크릿은 기동 시점에 예외로 드러난다.
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationSeconds = expirationHours * 3600;
    }

    /** 운영자 기준 토큰 발급. */
    public String generateToken(Operator operator) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expirationSeconds * 1000);
        return Jwts.builder()
                .subject(operator.getId().toString())
                .claim(CLAIM_USERNAME, operator.getUsername())
                .claim(CLAIM_ROLE, operator.getRole().name())
                .issuedAt(now)
                .expiration(expiry)
                .signWith(key)
                .compact();
    }

    /** 토큰 만료 시각(KST). 컨트롤러 응답의 expiresAt 산출용. */
    public ZonedDateTime resolveExpiry(String token) {
        Date exp = parseClaims(token).getExpiration();
        return ZonedDateTime.ofInstant(exp.toInstant(), java.time.ZoneId.of("Asia/Seoul"));
    }

    /**
     * 토큰 검증 후 인증 주체 추출. 만료/위조 시 {@link JwtException} 발생(필터에서 401 변환).
     */
    public AuthPrincipal parsePrincipal(String token) {
        Claims claims = parseClaims(token);
        UUID operatorId = UUID.fromString(claims.getSubject());
        String username = claims.get(CLAIM_USERNAME, String.class);
        OperatorRole role = OperatorRole.valueOf(claims.get(CLAIM_ROLE, String.class));
        return new AuthPrincipal(operatorId, username, role);
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
