package com.ggteam.cs.auth;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Spring Security 설정 (BR-37: 운영자 액션은 인증 필수).
 *
 * <p>인증 방식: 무상태(Stateless) JWT Bearer 토큰.
 * 비인증 허용(02-shared-contracts §4): {@code POST /api/v1/auth/login}, {@code POST /api/v1/inquiries}(고객 접수).
 * 그 외 모든 엔드포인트는 유효한 JWT 필요.
 *
 * <p>담당: 백엔드 A.
 */
@Configuration
public class SecurityConfig {

    private final JwtTokenProvider jwtTokenProvider;
    private final JwtAuthenticationEntryPoint authenticationEntryPoint;

    public SecurityConfig(JwtTokenProvider jwtTokenProvider, JwtAuthenticationEntryPoint authenticationEntryPoint) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.authenticationEntryPoint = authenticationEntryPoint;
    }

    // sim 프로파일은 SecurityAutoConfiguration을 제외(HttpSecurity 빈 없음)하고 엔드포인트를 개방하므로
    // 이 필터체인을 로드하지 않는다. 기본/운영 프로파일에는 영향 없음. passwordEncoder는 유지(AuthService 의존).
    @Bean
    @Profile("!sim")
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // 무상태 REST API: CSRF/세션/폼로그인/HTTP Basic 미사용
                .csrf(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // 비인증 허용 엔드포인트
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/login").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/inquiries").permitAll()
                        // 헬스체크/액추에이터
                        .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                        // 그 외 전부 인증 필요
                        .anyRequest().authenticated())
                .exceptionHandling(ex -> ex.authenticationEntryPoint(authenticationEntryPoint))
                .addFilterBefore(
                        new JwtAuthenticationFilter(jwtTokenProvider),
                        UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /** BCrypt 단방향 해시 (BR-36). */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
