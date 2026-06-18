package com.ggteam.cs.aipipeline.stub;

import com.ggteam.cs.aipipeline.AIAnalysisService;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link AIAnalysisService} stub 구현 — <b>백엔드 B 정식 구현 전 병렬 개발용.</b>
 *
 * <p>백엔드 C의 재분석(REANALYZE) 트리거 연동만 검증하면 되므로, 호출 로깅만 수행한다.
 * 실제 상태 전이(AI_ANALYZING)는 호출 측(ApprovalService)이 StateMachine으로 처리한다.
 * B의 정식 빈이 등록되면 {@code @ConditionalOnMissingBean}에 의해 대체된다.
 */
public class StubAIAnalysisService implements AIAnalysisService {

    private static final Logger log = LoggerFactory.getLogger(StubAIAnalysisService.class);

    @Override
    public void analyze(UUID inquiryId) {
        log.info("[stub] AI 분석 트리거 (inquiryId={}). 백엔드 B 정식 구현으로 대체 예정.", inquiryId);
    }
}
