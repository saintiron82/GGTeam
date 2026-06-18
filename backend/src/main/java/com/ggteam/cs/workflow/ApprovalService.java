package com.ggteam.cs.workflow;

import java.util.Optional;
import java.util.UUID;

/**
 * 운영자 워크플로우: Pull 배정, 승인/반려/재분석, 이력 (US-15~18).
 * Pull 배정은 원자적 조건부 갱신으로 동시성 보장 (BR-12, BR-14).
 *
 * <p>담당: 백엔드 C.
 */
public interface ApprovalService {

    /** 미배정 문의 1건을 운영자에게 Pull 배정. 가용 문의 없으면 empty. 반환: inquiryId. */
    Optional<UUID> pullAssign(UUID operatorId);

    /** 승인 → 발송 트리거. editedContent가 있으면 수정 후 승인. */
    void approve(UUID inquiryId, UUID operatorId, String editedContent);

    /** 반려 → AI 재생성 위임 (사유 필수, BR-16). 새 draftId 반환. */
    UUID reject(UUID inquiryId, UUID operatorId, String reason);

    /** 운영자 수동 재분석 요청 (REANALYZE). */
    void reanalyze(UUID inquiryId, UUID operatorId, String reason);
}
