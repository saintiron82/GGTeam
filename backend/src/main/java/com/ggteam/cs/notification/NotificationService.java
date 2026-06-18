package com.ggteam.cs.notification;

import java.util.UUID;

/**
 * 발송 및 알림 (US-12, US-25). 승인된 답변만 발송(BR-23), 실패 시 재시도(BR-25a).
 *
 * <p>담당: 백엔드 C.
 */
public interface NotificationService {

    /** 승인된 답변을 고객에게 발송하고 상태를 SENT로 전이. */
    void send(UUID inquiryId);
}
