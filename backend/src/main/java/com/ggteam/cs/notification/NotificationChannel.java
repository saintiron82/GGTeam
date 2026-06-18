package com.ggteam.cs.notification;

import java.util.UUID;

/**
 * 고객 답변 발송 채널 추상화. <b>담당: 백엔드 C.</b>
 *
 * <p>MVP는 시뮬레이션 채널({@link SimulatedNotificationChannel})을 사용한다.
 * 실제 채널(이메일/푸시/인앱 등) 연동 시 구현체를 교체한다.
 */
public interface NotificationChannel {

    /**
     * 답변 발송 시도. 발송 실패 시 예외를 던진다(재시도 대상, BR-25a).
     *
     * @param inquiryId 대상 문의
     * @param content   발송할 답변 본문
     */
    void dispatch(UUID inquiryId, String content);
}
