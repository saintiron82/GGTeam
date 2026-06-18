package com.ggteam.cs.auth.dto;

import com.ggteam.cs.common.enums.OperatorRole;
import com.ggteam.cs.persistence.entity.Operator;

import java.util.UUID;

/**
 * 응답용 운영자 요약 (id/username/role만 노출, passwordHash 등 민감정보 제외).
 */
public record OperatorSummary(UUID id, String username, OperatorRole role) {

    public static OperatorSummary from(Operator operator) {
        return new OperatorSummary(operator.getId(), operator.getUsername(), operator.getRole());
    }
}
