package com.ggteam.cs.external;

/**
 * LLM 호출 추상화 (NFR-03). BedrockClient가 구현하며, 로컬 테스트는 MockLlmClient 사용.
 * 타임아웃(점증 120/180/240s)·재시도(타임아웃 3회)·실패 유형 구분은 구현체 내부 책임.
 *
 * <p>담당: 백엔드 B. 구현 세부는 자유, 시그니처 변경은 팀 협의.
 */
public interface LlmClient {

    LlmResponse complete(LlmRequest request);

    /**
     * LLM 요청.
     * @param system 시스템 역할/지침 (없으면 null)
     * @param prompt 사용자 메시지 (컨텍스트 포함)
     * @param maxTokens 최대 출력 토큰
     */
    record LlmRequest(String system, String prompt, int maxTokens) {
        public static LlmRequest of(String prompt) {
            return new LlmRequest(null, prompt, 2048);
        }

        public static LlmRequest of(String system, String prompt, int maxTokens) {
            return new LlmRequest(system, prompt, maxTokens);
        }
    }

    /** LLM 응답. */
    record LlmResponse(String content) {}
}
