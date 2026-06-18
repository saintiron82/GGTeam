package com.ggteam.cs.dashboard.dto;

/**
 * 알림 집계 (01-api-contract §4, US-12).
 * unassignedCount: 미배정(PENDING_ASSIGNMENT) 건수, urgentCount: 긴급(HIGH) 미처리 건수.
 */
public record NotificationCounts(long unassignedCount, long urgentCount) {}
