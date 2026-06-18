package com.ggteam.cs.common.enums;

/** AI 분석 실패 유형. 정상 시 null. */
public enum FailureType {
    TIMEOUT,    // 타임아웃 (재시도 3회 소진 후 확정)
    API_ERROR   // API 오류 (즉시 실패)
}
