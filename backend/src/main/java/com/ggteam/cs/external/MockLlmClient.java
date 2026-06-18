package com.ggteam.cs.external;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * 로컬 개발/테스트용 LLM 클라이언트. AWS Bedrock 없이 파이프라인을 검증한다.
 * 활성화: application.yml의 app.ai.llm-client=mock (기본 local 개발값).
 *
 * <p>프롬프트에 포함된 키워드로 단계(분류/진단/초안)를 추정하여 그럴듯한 응답을 반환한다.
 * 실제 분류 품질이 아니라 파이프라인 흐름 검증이 목적이다.
 */
@Component
@ConditionalOnProperty(name = "app.ai.llm-client", havingValue = "mock")
public class MockLlmClient implements LlmClient {

    @Override
    public LlmResponse complete(LlmRequest request) {
        String prompt = request.prompt() == null ? "" : request.prompt();

        // 단계 추정: 프롬프트 지시어 기반
        if (prompt.contains("[CLASSIFY]")) {
            return new LlmResponse("""
                {
                  "aiType": "PAYMENT",
                  "subCategory": "아이템미지급",
                  "urgency": "HIGH",
                  "summary": "결제는 완료되었으나 아이템이 지급되지 않은 것으로 보이는 문의",
                  "keywords": ["결제완료", "아이템 미지급"]
                }
                """);
        }
        if (prompt.contains("[DIAGNOSE]")) {
            return new LlmResponse("""
                {
                  "cause": "결제는 정상 처리되었으나 시스템 오류로 아이템 지급이 누락됨",
                  "suggestedDirection": "누락된 아이템을 재지급하고 고객에게 안내",
                  "confidence": 0.9
                }
                """);
        }
        if (prompt.contains("[DRAFT]")) {
            return new LlmResponse(
                "확인 결과 결제는 정상적으로 완료되었으나 시스템 오류로 인해 아이템 지급이 "
                + "누락된 것으로 확인되었습니다. 누락된 아이템은 지급 처리되었으며, 이용에 "
                + "불편을 드린 점 양해 부탁드립니다.");
        }
        // 기본 응답
        return new LlmResponse("(mock) 처리할 수 없는 요청 형식입니다.");
    }
}
