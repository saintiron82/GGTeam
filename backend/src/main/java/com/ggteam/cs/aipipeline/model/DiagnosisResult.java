package com.ggteam.cs.aipipeline.model;

/**
 * AI 원인 진단 결과 값 객체 (엔티티 아님, US-11).
 * confidence는 0.0 ~ 1.0 (BR-05).
 */
public record DiagnosisResult(
        String cause,
        String suggestedDirection,
        double confidence
) {}
