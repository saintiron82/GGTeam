package com.ggteam.cs.dashboard.dto;

import java.util.List;
import org.springframework.data.domain.Page;

/**
 * 페이지네이션 응답 (01-api-contract §0).
 * { "data": [...], "page", "size", "totalElements", "totalPages" }
 */
public record PageResponse<T>(
        List<T> data,
        int page,
        int size,
        long totalElements,
        int totalPages) {

    public static <S, T> PageResponse<T> from(Page<S> source, List<T> mappedContent) {
        return new PageResponse<>(
                mappedContent,
                source.getNumber(),
                source.getSize(),
                source.getTotalElements(),
                source.getTotalPages());
    }
}
