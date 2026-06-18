package com.ggteam.cs.external;

/**
 * LLM 호출 추상화 (NFR-03). BedrockClient가 구현하며, 로컬 테스트는 MockLlmClient 사용.
 * 타임아웃(점증 120/180/240s)·재시도(타임아웃 3회)·실패 유형 구분은 구현체 내부 책임.
 *
 * <p>담당: 백엔드 B. 구현 세부는 자유, 시그니처 변경은 팀 협의.
 */
public interface LlmClient {

    LlmResponse complete(LlmRequest request);

    /** LLM 요청. prompt는 시스템 지시 + 사용자 컨텍스트를 합성한 최종 입력. */
    record LlmRequest(String prompt, int maxTokens) {
        public static LlmRequest of(String prompt) {
            return new LlmRequest(prompt, 2048);
        }
    }

    /** LLM 응답. */
    record LlmResponse(String content) {}
}
