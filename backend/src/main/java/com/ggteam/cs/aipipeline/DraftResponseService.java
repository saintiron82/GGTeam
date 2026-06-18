package com.ggteam.cs.aipipeline;

import java.util.UUID;

/**
 * 답변 초안 생성/재생성 (US-13, US-14, US-17).
 * 반려 사유를 입력으로 받아 재생성에 반영(BR-17). 재생성 한도 3회(BR-19).
 *
 * <p>담당: 백엔드 B.
 */
public interface DraftResponseService {

    /** 진단 직후 초안 자동 생성. 생성된 draftId 반환. */
    UUID generate(UUID inquiryId);

    /** 반려 사유를 반영하여 재생성. 새 draftId 반환. */
    UUID regenerate(UUID inquiryId, String rejectReason);
}
