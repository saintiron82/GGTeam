package com.ggteam.cs.external;

/** LLM 호출 타임아웃 (재시도 소진). FailureType.TIMEOUT에 매핑 (BR-26). */
public class LlmTimeoutException extends RuntimeException {
    public LlmTimeoutException(String message, Throwable cause) {
        super(message, cause);
    }
}
