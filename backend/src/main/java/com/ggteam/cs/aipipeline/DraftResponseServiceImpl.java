package com.ggteam.cs.aipipeline;

import com.ggteam.cs.aipipeline.parse.LlmResponseParser;
import com.ggteam.cs.aipipeline.prompt.PromptBuilder;
import com.ggteam.cs.aipipeline.quality.ResponseQualityValidator;
import com.ggteam.cs.common.BusinessException;
import com.ggteam.cs.common.ErrorCode;
import com.ggteam.cs.common.enums.DraftResponseStatus;
import com.ggteam.cs.external.LlmClient;
import com.ggteam.cs.persistence.entity.Diagnosis;
import com.ggteam.cs.persistence.entity.DraftResponse;
import com.ggteam.cs.persistence.repository.DiagnosisRepository;
import com.ggteam.cs.persistence.repository.DraftResponseRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * 답변 초안 생성/재생성 정식 구현 (US-13/14/17). 백엔드 B.
 * 진단 결과 기반으로 LLM 답변을 생성하고 품질 검증(BR-30d~f) + 재생성 한도(BR-19)를 적용한다.
 *
 * <p>@Service 등록으로 StubDraftResponseService(@ConditionalOnMissingBean)를 대체한다.
 */
@Service
public class DraftResponseServiceImpl implements DraftResponseService {

    private static final Logger log = LoggerFactory.getLogger(DraftResponseServiceImpl.class);

    private final LlmClient llmClient;
    private final PromptBuilder promptBuilder;
    private final ResponseQualityValidator qualityValidator;
    private final DiagnosisRepository diagnosisRepository;
    private final DraftResponseRepository draftResponseRepository;
    private final int maxRegeneration;

    public DraftResponseServiceImpl(LlmClient llmClient,
                                    PromptBuilder promptBuilder,
                                    ResponseQualityValidator qualityValidator,
                                    DiagnosisRepository diagnosisRepository,
                                    DraftResponseRepository draftResponseRepository,
                                    @Value("${app.draft.max-regeneration:3}") int maxRegeneration) {
        this.llmClient = llmClient;
        this.promptBuilder = promptBuilder;
        this.qualityValidator = qualityValidator;
        this.diagnosisRepository = diagnosisRepository;
        this.draftResponseRepository = draftResponseRepository;
        this.maxRegeneration = maxRegeneration;
    }

    @Override
    @Transactional
    public UUID generate(UUID inquiryId) {
        Diagnosis diagnosis = diagnosisRepository.findByInquiryId(inquiryId)
                .orElseThrow(() -> new BusinessException(ErrorCode.INTERNAL_ERROR, "진단 결과가 없습니다."));
        String content = generateContent(diagnosis.getCause(), diagnosis.getSuggestedDirection(), null);
        DraftResponse draft = DraftResponse.of(inquiryId, content, 0);
        return draftResponseRepository.save(draft).getId();
    }

    @Override
    @Transactional
    public UUID regenerate(UUID inquiryId, String rejectReason) {
        DraftResponse current = draftResponseRepository
                .findTopByInquiryIdOrderByCreatedAtDesc(inquiryId)
                .orElseThrow(() -> new BusinessException(ErrorCode.INTERNAL_ERROR, "재생성할 기존 초안이 없습니다."));

        int nextCount = current.getRegenerationCount() + 1;
        if (nextCount > maxRegeneration) {
            throw new BusinessException(ErrorCode.REGENERATION_LIMIT_EXCEEDED);
        }

        current.setStatus(DraftResponseStatus.REJECTED);
        draftResponseRepository.save(current);

        Diagnosis diagnosis = diagnosisRepository.findByInquiryId(inquiryId)
                .orElseThrow(() -> new BusinessException(ErrorCode.INTERNAL_ERROR, "진단 결과가 없습니다."));
        String content = generateContent(diagnosis.getCause(), diagnosis.getSuggestedDirection(), rejectReason);

        DraftResponse regenerated = DraftResponse.of(inquiryId, content, nextCount);
        return draftResponseRepository.save(regenerated).getId();
    }

    /** LLM 답변 생성 + 품질 검증(불량 시 1회 재시도). */
    private String generateContent(String cause, String direction, String rejectReason) {
        LlmClient.LlmResponse res = llmClient.complete(promptBuilder.draft(cause, direction, rejectReason));
        ResponseQualityValidator.QualityResult quality = qualityValidator.validate(res.content());
        if (!quality.valid()) {
            log.warn("답변 품질 불량({}), 1회 재시도", quality.reason());
            res = llmClient.complete(promptBuilder.draft(cause, direction, rejectReason));
            ResponseQualityValidator.QualityResult retry = qualityValidator.validate(res.content());
            if (!retry.valid()) {
                // 재시도 후에도 불량: 안내 문구로 대체(운영자 수정 전제)
                return "[자동 생성 실패 - 운영자 작성 필요] 진단: " + cause;
            }
        }
        return res.content();
    }
}
