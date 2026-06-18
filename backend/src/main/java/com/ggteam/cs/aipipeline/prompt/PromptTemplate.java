package com.ggteam.cs.aipipeline.prompt;

/**
 * 외부화된 프롬프트 템플릿 (JSON 파일 매핑).
 * resources/prompts/{id}.json 또는 외부 디렉토리(app.prompts.dir)에서 로드.
 */
public record PromptTemplate(
        String id,
        String description,
        String system,
        String userTemplate,
        int maxTokens
) {}
