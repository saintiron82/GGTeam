package com.ggteam.cs.notification;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 알림/발송 모듈 빈 구성. <b>담당: 백엔드 C.</b>
 */
@Configuration
public class NotificationConfig {

    /** 실제 발송 채널이 없으면 시뮬레이션 채널을 사용. */
    @Bean
    @ConditionalOnMissingBean(NotificationChannel.class)
    public NotificationChannel simulatedNotificationChannel() {
        return new SimulatedNotificationChannel();
    }
}
