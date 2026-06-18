package com.ggteam.cs.aipipeline.prompt;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 프롬프트 템플릿 로더 (US-06/11/13).
 *
 * <p>로드 우선순위:
 * <ol>
 *   <li>app.prompts.dir 설정 시: 해당 외부 디렉토리의 {id}.json (런타임 편집 즉시 반영, 캐시 안 함)</li>
 *   <li>미설정 시: classpath:prompts/{id}.json (1회 로드 후 캐시)</li>
 * </ol>
 * 프롬프트를 코드와 분리하여 재빌드 없이 튜닝 가능하게 한다.
 */
@Component
public class PromptLoader {

    private final ObjectMapper mapper = new ObjectMapper();
    private final Map<String, PromptTemplate> cache = new ConcurrentHashMap<>();
    private final String externalDir;

    public PromptLoader(@Value("${app.prompts.dir:}") String externalDir) {
        this.externalDir = externalDir == null ? "" : externalDir.trim();
    }

    public PromptTemplate load(String id) {
        // 외부 디렉토리 우선: 매번 로드(편집 즉시 반영)
        if (!externalDir.isEmpty()) {
            return loadFromExternal(id);
        }
        // classpath: 캐시
        return cache.computeIfAbsent(id, this::loadFromClasspath);
    }

    private PromptTemplate loadFromExternal(String id) {
        try {
            Path path = Path.of(externalDir, id + ".json");
            try (InputStream in = Files.newInputStream(path)) {
                return mapper.readValue(in, PromptTemplate.class);
            }
        } catch (Exception e) {
            throw new IllegalStateException("외부 프롬프트 로드 실패: " + id + " (" + externalDir + ")", e);
        }
    }

    private PromptTemplate loadFromClasspath(String id) {
        try (InputStream in = new ClassPathResource("prompts/" + id + ".json").getInputStream()) {
            return mapper.readValue(in, PromptTemplate.class);
        } catch (Exception e) {
            throw new IllegalStateException("프롬프트 로드 실패: prompts/" + id + ".json", e);
        }
    }
}
