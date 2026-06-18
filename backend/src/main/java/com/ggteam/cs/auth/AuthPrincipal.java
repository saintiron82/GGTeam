package com.ggteam.cs.auth;

import com.ggteam.cs.common.enums.OperatorRole;

import java.util.UUID;

/**
 * 인증된 운영자 식별 정보. SecurityContext의 Authentication principal로 저장된다.
 * 컨트롤러/서비스는 {@code @AuthenticationPrincipal AuthPrincipal} 로 주입받아 사용.
 *
 * @param operatorId 운영자 UUID (JWT sub)
 * @param username   로그인 ID
 * @param role       운영자 역할
 */
public record AuthPrincipal(UUID operatorId, String username, OperatorRole role) {}
