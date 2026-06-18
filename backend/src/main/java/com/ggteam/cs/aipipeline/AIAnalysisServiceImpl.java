package com.ggteam.cs.aipipeline;

import com.ggteam.cs.aipipeline.model.AnalysisResult;
import com.ggteam.cs.aipipeline.model.DiagnosisResult;
import com.ggteam.cs.aipipeline.parse.LlmResponseParser;
import com.ggteam.cs.aipipeline.prompt.PromptBuilder;
import com.ggteam.cs.aipipeline.query.QueryStrategy;
import com.ggteam.cs.aipipeline.query.SystemDataQueryService;
import com.ggteam.cs.common.enums.FailureType;
import com.ggteam.cs.common.enums.InquiryStatus;
import com.ggteam.cs.external.LlmApiException;
import com.ggteam.cs.external.LlmClient;
import com.ggteam.cs.external.LlmTimeoutException;
import com.ggteam.cs.persistence.entity.AIAnalysis;
import com.ggteam.cs.persistence.entity.Diagnosis;
import com.ggteam.cs.persistence.entity.Inquiry;
import com.ggteam.cs.persistence.repository.AIAnalysisRepository;
import com.ggteam.cs.persistence.repository.DiagnosisRepository;
import com.ggteam.cs.persistence.repository.InquiryRepository;
import com.ggteam.cs.workflow.InquiryStateMachine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;

/**
 * AI 분석 파이프라인 정식 구현 (US-06~08, US-11). 백엔드 B.
 * 분류 → 시스템 조회 → 진단 → 답변 초안 생성을 비동기 오케스트레이션한다.
 * 실패(타임아웃/ API 에러) 시 FailureType 기록 후 MANUAL_CLASSIFICATION_PENDING 전이(BR-28).
 *
 * <p>@Service 등록으로 StubAIAnalysisService(@ConditionalOnMissingBean)를 대체한다.
 */
@Service
public class AIAnalysisServiceImpl implements AIAnalysisService {

    private static final Logger log = LoggerFactory.getLogger(AIAnalysisServiceImpl.class);

    private final LlmClient llmClient;
    private final PromptBuilder promptBuilder;
    private final LlmResponseParser parser;
    private final SystemDataQueryService systemDataQueryService;
    private final DiagnosisService diagnosisService;
    private final DraftResponseService draftResponseService;
    private final InquiryRepository inquiryRepository;
    private final AIAnalysisRepository aiAnalysisRepository;
    private final DiagnosisRepository diagnosisRepository;
    private final InquiryStateMachine stateMachine;

    public AIAnalysisServiceImpl(LlmClient llmClient, PromptBuilder promptBuilder, LlmResponseParser parser,
                                 SystemDataQueryService systemDataQueryService, DiagnosisService diagnosisService,
                                 DraftResponseService draftResponseService, InquiryRepository inquiryRepository,
                                 AIAnalysisRepository aiAnalysisRepository, DiagnosisRepository diagnosisRepository,
                                 InquiryStateMachine stateMachine) {
        this.llmClient = llmClient;
        this.promptBuilder = promptBuilder;
        this.parser = parser;
        this.systemDataQueryService = systemDataQueryService;
        this.diagnosisService = diagnosisService;
        this.draftResponseService = draftResponseService;
        this.inquiryRepository = inquiryRepository;
        this.aiAnalysisRepository = aiAnalysisRepository;
        this.diagnosisRepository = diagnosisRepository;
        this.stateMachine = stateMachine;
    }

    @Override
    @Async
    public void analyze(UUID inquiryId) {
        try {
            Inquiry inquiry = inquiryRepository.findById(inquiryId).orElseThrow();
            String userId = extractUserId(inquiry);

            stateMachine.transition(inquiryId, InquiryStatus.AI_ANALYZING, null, null);

            // 1) 분류
            AnalysisResult analysis = parser.parseAnalysis(
                    llmClient.complete(promptBuilder.classify(inquiry.getContent())).content());

            // 2) 시스템 조회
            QueryStrategy.SystemQueryResult sqr = systemDataQueryService.query(analysis.aiType(), userId);

            // 3) 분석 결과 저장
            aiAnalysisRepository.save(AIAnalysis.success(
                    inquiryId, analysis.aiType(), analysis.subCategory(), analysis.urgency(),
                    analysis.summary(), analysis.keywords(), sqr.data()));

            // 4) 진단
            DiagnosisResult dr = diagnosisService.diagnose(analysis.summary(), sqr.data());
            diagnosisRepository.save(Diagnosis.of(inquiryId, dr.cause(), dr.suggestedDirection(), dr.confidence()));

            // 5) 답변 초안 자동 생성
            draftResponseService.generate(inquiryId);

            // 6) 완료 → 담당자 배정 대기
            stateMachine.transition(inquiryId, InquiryStatus.PENDING_ASSIGNMENT, null, null);
            log.info("[ai-pipeline] 분석 완료 inquiryId={} type={} urgency={}",
                    inquiryId, analysis.aiType(), analysis.urgency());

        } catch (LlmTimeoutException e) {
            fail(inquiryId, FailureType.TIMEOUT, e);
        } catch (LlmApiException e) {
            fail(inquiryId, FailureType.API_ERROR, e);
        } catch (Exception e) {
            fail(inquiryId, FailureType.API_ERROR, e);
        }
    }

    /** 실패 처리: FailureType 기록 + 수동분류대기 전이 (BR-28). */
    private void fail(UUID inquiryId, FailureType failureType, Exception e) {
        log.warn("[ai-pipeline] 분석 실패 inquiryId={} type={} cause={}", inquiryId, failureType, e.getMessage());
        try {
            AIAnalysis analysis = aiAnalysisRepository.findByInquiryId(inquiryId)
                    .orElseGet(() -> AIAnalysis.failed(inquiryId, failureType));
            analysis.setFailureType(failureType);
            aiAnalysisRepository.save(analysis);
            stateMachine.transition(inquiryId, InquiryStatus.MANUAL_CLASSIFICATION_PENDING, null, null);
        } catch (Exception inner) {
            log.error("[ai-pipeline] 실패 처리 중 오류 inquiryId={}: {}", inquiryId, inner.getMessage());
        }
    }

    private String extractUserId(Inquiry inquiry) {
        Map<String, Object> info = inquiry.getCustomerInfo();
        if (info != null && info.get("userId") != null) {
            return String.valueOf(info.get("userId"));
        }
        return "unknown";
    }
}
