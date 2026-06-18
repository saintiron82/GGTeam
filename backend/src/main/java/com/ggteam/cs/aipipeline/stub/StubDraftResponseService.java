package com.ggteam.cs.aipipeline.stub;

import com.ggteam.cs.aipipeline.DraftResponseService;
import com.ggteam.cs.common.BusinessException;
import com.ggteam.cs.common.ErrorCode;
import com.ggteam.cs.common.enums.DraftResponseStatus;
import com.ggteam.cs.persistence.entity.DraftResponse;
import com.ggteam.cs.persistence.repository.DraftResponseRepository;
import java.util.UUID;

/**
 * {@link DraftResponseService} stub 구현 — <b>백엔드 B 정식 구현 전 병렬 개발용.</b>
 *
 * <p>백엔드 C의 반려→재생성 워크플로우(BR-17~19)를 검증할 수 있도록, 재생성 시
 * regenerationCount를 증가시키고 한도(3회, BR-19)를 적용한 새 초안을 생성한다.
 * B의 정식 빈이 등록되면 {@code @ConditionalOnMissingBean}에 의해 대체된다.
 */
public class StubDraftResponseService implements DraftResponseService {

    private static final int MAX_REGENERATION = 3;

    private final DraftResponseRepository draftResponseRepository;

    public StubDraftResponseService(DraftResponseRepository draftResponseRepository) {
        this.draftResponseRepository = draftResponseRepository;
    }

    @Override
    public UUID generate(UUID inquiryId) {
        DraftResponse draft = DraftResponse.of(inquiryId,
                "[stub] 자동 생성된 답변 초안입니다.", 0);
        return draftResponseRepository.save(draft).getId();
    }

    @Override
    public UUID regenerate(UUID inquiryId, String rejectReason) {
        DraftResponse current = draftResponseRepository
                .findTopByInquiryIdOrderByCreatedAtDesc(inquiryId)
                .orElseThrow(() -> new BusinessException(ErrorCode.INQUIRY_NOT_FOUND,
                        "재생성할 기존 초안이 없습니다."));

        int nextCount = current.getRegenerationCount() + 1;
        if (nextCount > MAX_REGENERATION) {
            throw new BusinessException(ErrorCode.REGENERATION_LIMIT_EXCEEDED);
        }

        // 기존 초안 반려 처리 (BR-17)
        current.setStatus(DraftResponseStatus.REJECTED);
        draftResponseRepository.save(current);

        DraftResponse regenerated = DraftResponse.of(inquiryId,
                "[stub] 반려 사유 반영 재생성 초안: " + rejectReason, nextCount);
        return draftResponseRepository.save(regenerated).getId();
    }
}
