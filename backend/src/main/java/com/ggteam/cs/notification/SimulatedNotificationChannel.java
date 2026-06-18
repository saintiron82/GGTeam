package com.ggteam.cs.notification;

import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * MVP용 시뮬레이션 발송 채널. <b>담당: 백엔드 C.</b>
 *
 * <p>실제 외부 채널이 없는 환경에서 발송을 로깅으로 모사한다. 실제 채널 구현체가 등록되면
 * {@code NotificationConfig}의 {@code @ConditionalOnMissingBean}에 의해 대체된다.
 */
public class SimulatedNotificationChannel implements NotificationChannel {

    private static final Logger log = LoggerFactory.getLogger(SimulatedNotificationChannel.class);

    @Override
    public void dispatch(UUID inquiryId, String content) {
        log.info("[notification] 답변 발송(시뮬레이션) inquiryId={}, length={}",
                inquiryId, content != null ? content.length() : 0);
    }
}
