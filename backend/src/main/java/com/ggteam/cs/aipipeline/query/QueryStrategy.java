package com.ggteam.cs.aipipeline.query;

import com.ggteam.cs.common.enums.InquiryType;

import java.util.Map;

/**
 * 문의 유형별 시스템 데이터 조회 전략 (Strategy Pattern, NFR-03/SCAL-01).
 * MVP는 PaymentQueryStrategy만 구현. 신규 유형은 구현체 추가만으로 확장.
 *
 * <p>담당: 백엔드 B. Spring Bean으로 등록되어 Map&lt;InquiryType, QueryStrategy&gt;로 주입.
 */
public interface QueryStrategy {

    /** 이 전략이 처리하는 문의 유형. */
    InquiryType supportedType();

    /** userId 기준 관련 시스템 데이터 조회 후 정규화된 결과 반환. */
    SystemQueryResult query(String userId);

    /** 조회 결과 표준 컨테이너. data는 systemQueryResult(JSON)로 직렬화된다. */
    record SystemQueryResult(InquiryType type, Map<String, Object> data) {}
}
