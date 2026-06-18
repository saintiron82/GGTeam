package com.ggteam.cs.workflow.stub;

import com.ggteam.cs.aipipeline.AIAnalysisService;
import com.ggteam.cs.aipipeline.DraftResponseService;
import com.ggteam.cs.aipipeline.stub.StubAIAnalysisService;
import com.ggteam.cs.aipipeline.stub.StubDraftResponseService;
import com.ggteam.cs.persistence.repository.DraftResponseRepository;
import com.ggteam.cs.persistence.repository.InquiryRepository;
import com.ggteam.cs.workflow.InquiryStateMachine;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 백엔드 C 병렬 개발용 stub 빈 등록 (05-parallel-work-plan M1).
 *
 * <p>백엔드 A({@link InquiryStateMachine})와 백엔드 B({@link DraftResponseService},
 * {@link AIAnalysisService})의 정식 구현이 아직 없는 동안, C가 독립적으로 빌드/테스트할 수
 * 있도록 stub 빈을 제공한다. 각 빈은 {@link ConditionalOnMissingBean}으로 보호되어,
 * A/B의 정식 빈이 컨텍스트에 등록되면 자동으로 비활성화된다(머지 후 코드 변경 불필요).
 */
@Configuration
public class BackendCStubConfig {

    @Bean
    @ConditionalOnMissingBean(InquiryStateMachine.class)
    public InquiryStateMachine stubInquiryStateMachine(InquiryRepository inquiryRepository) {
        return new StubInquiryStateMachine(inquiryRepository);
    }

    @Bean
    @ConditionalOnMissingBean(DraftResponseService.class)
    public DraftResponseService stubDraftResponseService(DraftResponseRepository draftResponseRepository) {
        return new StubDraftResponseService(draftResponseRepository);
    }

    @Bean
    @ConditionalOnMissingBean(AIAnalysisService.class)
    public AIAnalysisService stubAIAnalysisService() {
        return new StubAIAnalysisService();
    }
}
