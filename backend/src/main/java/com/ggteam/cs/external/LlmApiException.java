package com.ggteam.cs.external;

/** LLM API 오류 (인증/요청 오류 등). 재시도하지 않음. FailureType.API_ERROR에 매핑 (BR-27). */
public class LlmApiException extends RuntimeException {
    public LlmApiException(String message, Throwable cause) {
        super(message, cause);
    }
}
