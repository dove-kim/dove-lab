package com.dove.screening.domain.value;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * 개별 종목 조건 — 특정 종목을 직접 포함·제외하는 조건.
 *
 * @param marketType 시장 타입 이름 ("KOSPI" 또는 "KOSDAQ")
 * @param stockCode  종목 코드 (예: "005930")
 * @param mode       포함("INCLUDE") 또는 제외("EXCLUDE")
 */
public record StockCondition(
        String marketType,
        String stockCode,
        String mode
) {
    public static StockCondition include(String marketType, String stockCode) {
        return new StockCondition(marketType, stockCode, "INCLUDE");
    }

    public static StockCondition exclude(String marketType, String stockCode) {
        return new StockCondition(marketType, stockCode, "EXCLUDE");
    }

    @JsonIgnore
    public boolean isInclude() {
        return "INCLUDE".equals(mode);
    }

    @JsonIgnore
    public boolean isExclude() {
        return "EXCLUDE".equals(mode);
    }
}
