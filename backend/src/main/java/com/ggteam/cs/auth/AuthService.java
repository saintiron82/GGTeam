package com.ggteam.cs.auth;

import com.ggteam.cs.auth.dto.LoginRequest;
import com.ggteam.cs.auth.dto.LoginResponse;
import com.ggteam.cs.auth.dto.OperatorSummary;
import com.ggteam.cs.common.BusinessException;
import com.ggteam.cs.common.ErrorCode;
import com.ggteam.cs.persistence.entity.Operator;
import com.ggteam.cs.persistence.repository.OperatorRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 인증 서비스: 로그인, 계정 잠금/실패 카운트 관리 (BR-31~38).
 *
 * <p>담당: 백엔드 A.
 */
@Service
public class AuthService {

    private final OperatorRepository operatorRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final int maxFailedLogin;

    public AuthService(
            OperatorRepository operatorRepository,
            PasswordEncoder passwordEncoder,
            JwtTokenProvider jwtTokenProvider,
            @Value("${app.auth.max-failed-login:5}") int maxFailedLogin) {
        this.operatorRepository = operatorRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
        this.maxFailedLogin = maxFailedLogin;
    }

    /**
     * 로그인 처리.
     *
     * <ul>
     *   <li>존재하지 않는 사용자 / 비밀번호 불일치 → INVALID_CREDENTIALS (401)</li>
     *   <li>잠긴 계정 → ACCOUNT_LOCKED (423) (BR-32)</li>
     *   <li>연속 실패 {@code maxFailedLogin}회 도달 시 잠금 (BR-31)</li>
     *   <li>성공 시 실패 카운트 0으로 초기화 (BR-33), JWT 발급 (BR-34)</li>
     * </ul>
     */
    @Transactional
    public LoginResponse login(LoginRequest request) {
        Operator operator = operatorRepository.findByUsername(request.username())
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_CREDENTIALS));

        // 잠긴 계정은 비밀번호 일치 여부와 무관하게 차단 (BR-32)
        if (operator.isLocked()) {
            throw new BusinessException(ErrorCode.ACCOUNT_LOCKED);
        }

        if (!passwordEncoder.matches(request.password(), operator.getPasswordHash())) {
            registerFailure(operator);
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
        }

        // 성공: 실패 카운트 초기화 (BR-33)
        if (operator.getFailedLoginCount() != 0) {
            operator.setFailedLoginCount(0);
            operatorRepository.save(operator);
        }

        String token = jwtTokenProvider.generateToken(operator);
        return new LoginResponse(token, OperatorSummary.from(operator), jwtTokenProvider.resolveExpiry(token));
    }

    /** 로그인 실패 1회 반영. 임계치 도달 시 잠금 (BR-31). */
    private void registerFailure(Operator operator) {
        int next = operator.getFailedLoginCount() + 1;
        operator.setFailedLoginCount(next);
        if (next >= maxFailedLogin) {
            operator.setLocked(true);
        }
        operatorRepository.save(operator);
    }
}
