package com.ggteam.cs.aipipeline;

import java.util.UUID;

/**
 * AI 분석 파이프라인 진입점 (US-06~08). 비동기로 분류→조회→진단→초안생성을 오케스트레이션.
 *
 * <p>담당: 백엔드 B.
 */
public interface AIAnalysisService {

    /** 비동기 분석 트리거. 문의 상태를 AI_ANALYZING으로 전이 후 파이프라인 실행. */
    void analyze(UUID inquiryId);
}
