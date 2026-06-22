package com.ggteam.cs.dashboard.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.Map;

/**
 * 대시보드 통계 (US-27). 상태별/유형별 분포 및 핵심 지표.
 */
@JsonInclude(JsonInclude.Include.ALWAYS)
public record DashboardStats(
        long total,            // 전체 문의 수
        long todayCount,       // 오늘(KST) 접수 수
        long unassigned,       // 미배정(담당자배정대기)
        long urgent,           // 긴급(HIGH) 미발송
        long inProgress,       // 처리중(접수~승인완료, SENT 제외)
        long completed,        // 발송완료(SENT)
        Map<String, Long> statusCounts,  // 상태별 카운트
        Map<String, Long> typeCounts) {} // 유형별 카운트(customerType)
