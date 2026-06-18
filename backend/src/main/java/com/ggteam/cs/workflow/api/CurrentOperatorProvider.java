package com.ggteam.cs.workflow.api;

import com.ggteam.cs.auth.AuthPrincipal;
import com.ggteam.cs.common.BusinessException;
import com.ggteam.cs.common.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * 현재 요청의 운영자 식별자 해석기. <b>담당: 백엔드 C (백엔드 A 보안 연동 추상화).</b>
 *
 * <p>정식 경로는 백엔드 A의 JWT 필터가 채운 SecurityContext의 principal(operatorId)이다.
 * A의 보안 설정이 머지되기 전 C의 독립 개발/테스트를 위해, 인증 컨텍스트가 없으면
 * {@code X-Operator-Id} 헤더(stub)로 폴백한다. 두 경로 모두 없으면 UNAUTHORIZED.
 */
@Component
public class CurrentOperatorProvider {

    public static final String STUB_HEADER = "X-Operator-Id";

    /** 현재 운영자 ID. 없으면 BusinessException(UNAUTHORIZED). */
    public UUID currentOperatorId() {
        UUID fromContext = fromSecurityContext();
        if (fromContext != null) {
            return fromContext;
        }
        UUID fromHeader = fromStubHeader();
        if (fromHeader != null) {
            return fromHeader;
        }
        throw new BusinessException(ErrorCode.UNAUTHORIZED);
    }

    private UUID fromSecurityContext() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return null;
        }
        // 정식 경로: A의 JWT 필터가 채운 principal(AuthPrincipal)에서 operatorId 추출
        if (auth.getPrincipal() instanceof AuthPrincipal principal) {
            return principal.operatorId();
        }
        // 폴백: principal name이 UUID 문자열인 경우
        return auth.getName() == null ? null : tryParse(auth.getName());
    }

    private UUID fromStubHeader() {
        if (!(RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attrs)) {
            return null;
        }
        HttpServletRequest request = attrs.getRequest();
        return tryParse(request.getHeader(STUB_HEADER));
    }

    private UUID tryParse(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}
