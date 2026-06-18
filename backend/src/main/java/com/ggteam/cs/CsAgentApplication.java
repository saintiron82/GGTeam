package com.ggteam.cs;

import jakarta.annotation.PostConstruct;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

import java.util.TimeZone;

/**
 * AI 기반 CS 문의 처리 에이전트 - Backend 진입점.
 *
 * <p>모든 시각 데이터는 KST(Asia/Seoul) 기준으로 처리한다 (BR-41).
 */
@SpringBootApplication
@EnableAsync
public class CsAgentApplication {

    public static void main(String[] args) {
        SpringApplication.run(CsAgentApplication.class, args);
    }

    /** JVM 기본 타임존을 KST로 고정. */
    @PostConstruct
    public void init() {
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Seoul"));
    }
}
