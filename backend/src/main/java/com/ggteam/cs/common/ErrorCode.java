package com.ggteam.cs.common;

import org.springframework.http.HttpStatus;

/**
 * 공유 에러코드 (02-shared-contracts §3).
 * 프론트엔드와 합의된 코드. 변경 시 양측 협의 필수.
 */
public enum ErrorCode {
    VALIDATION_ERROR(HttpStatus.BAD_REQUEST, "입력 검증에 실패했습니다."),
    REASON_REQUIRED(HttpStatus.BAD_REQUEST, "반려 사유는 필수입니다."),
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "아이디 또는 비밀번호가 올바르지 않습니다."),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "인증이 필요합니다."),
    FORBIDDEN(HttpStatus.FORBIDDEN, "권한이 없습니다."),
    INQUIRY_NOT_FOUND(HttpStatus.NOT_FOUND, "문의를 찾을 수 없습니다."),
    INVALID_STATE_TRANSITION(HttpStatus.CONFLICT, "허용되지 않은 상태 전이입니다."),
    REGENERATION_LIMIT_EXCEEDED(HttpStatus.CONFLICT, "재생성 한도를 초과했습니다."),
    ASSIGNMENT_CONFLICT(HttpStatus.CONFLICT, "이미 배정된 문의입니다."),
    ACCOUNT_LOCKED(HttpStatus.LOCKED, "계정이 잠겼습니다. 관리자에게 문의하세요."),
    AI_SERVICE_ERROR(HttpStatus.BAD_GATEWAY, "AI 서비스 호출에 실패했습니다."),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 오류가 발생했습니다.");

    private final HttpStatus status;
    private final String defaultMessage;

    ErrorCode(HttpStatus status, String defaultMessage) {
        this.status = status;
        this.defaultMessage = defaultMessage;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getDefaultMessage() {
        return defaultMessage;
    }
}
