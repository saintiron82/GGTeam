package com.ggteam.cs.aipipeline.query;

import com.ggteam.cs.common.enums.InquiryType;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 시스템 데이터 조회 진입점 (Strategy Context, US-09/10, NFR-03). 백엔드 B.
 * 등록된 QueryStrategy들을 유형별로 매핑하여 위임한다. 미지원 유형은 빈 결과.
 */
@Service
public class SystemDataQueryService {

    private final Map<InquiryType, QueryStrategy> strategies;

    public SystemDataQueryService(List<QueryStrategy> strategyList) {
        this.strategies = strategyList.stream()
                .collect(Collectors.toMap(QueryStrategy::supportedType, Function.identity(), (a, b) -> a));
    }

    /** 유형에 맞는 전략으로 조회. 미지원 유형이면 빈 결과 반환. */
    public QueryStrategy.SystemQueryResult query(InquiryType type, String userId) {
        QueryStrategy strategy = strategies.get(type);
        if (strategy == null) {
            return new QueryStrategy.SystemQueryResult(type, Map.of("supported", false));
        }
        return strategy.query(userId);
    }
}
