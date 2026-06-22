package com.ggteam.cs.dashboard.dto;

import java.util.UUID;

/**
 * 운영자별 처리 현황 (US-27 확장).
 *
 * @param operatorId 운영자 식별자
 * @param username   로그인 ID
 * @param role       권한 (OPERATOR/ADMIN)
 * @param assigned   현재 담당 중인 문의 수 (assignedOperatorId 기준)
 * @param approved   승인 완료한 문의 수 (APPROVE 액션 기준, 중복 제외)
 * @param actions    총 처리 액션 수 (배정/승인/수정/반려/재분석 등)
 */
public record OperatorStat(
        UUID operatorId,
        String username,
        String role,
        long assigned,
        long approved,
        long actions) {}
