package com.ggteam.cs.aipipeline.quality;

import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 답변 초안 자동 품질 검증 (순수 로직, BR-30d~f).
 * 불량 판정 시 호출측(DraftResponseService)이 재생성/수동분류대기로 분기한다.
 *
 * <p>담당: 백엔드 B.
 */
@Component
public class ResponseQualityValidator {

    private static final int MIN_LENGTH = 20;
    // 데모용 금칙어. 운영 시 정책에 맞게 확장.
    private static final List<String> BANNED = List.of("씨발", "바보", "fuck");

    public record QualityResult(boolean valid, String reason) {
        public static QualityResult ok() { return new QualityResult(true, null); }
        public static QualityResult fail(String reason) { return new QualityResult(false, reason); }
    }

    /** 답변 초안 품질 검증. */
    public QualityResult validate(String draft) {
        if (draft == null || draft.isBlank()) {
            return QualityResult.fail("빈 응답");
        }
        String trimmed = draft.trim();
        if (trimmed.length() < MIN_LENGTH) {
            return QualityResult.fail("최소 길이(" + MIN_LENGTH + "자) 미달: " + trimmed.length() + "자");
        }
        String lower = trimmed.toLowerCase();
        for (String banned : BANNED) {
            if (lower.contains(banned.toLowerCase())) {
                return QualityResult.fail("금칙어 포함: " + banned);
            }
        }
        return QualityResult.ok();
    }
}
