package com.dove.screening.domain.value;

/**
 * 검색식 leaf 조건의 종류.
 */
public enum FilterConditionType {
    INDICATOR_VALUE,
    INDICATOR_RANGE,
    INDICATOR_CROSS,
    PRICE_VALUE,
    PRICE_RANGE,
    VOLUME_VALUE,
    VOLUME_RANGE,
    TURNOVER_VALUE,
    TURNOVER_RANGE,
    PRICE_VS_INDICATOR,
    MARKET_FILTER,
    MODEL_SCORE_VALUE,
    MODEL_SCORE_RANGE,
    RANK_VALUE,
    RANK_RANGE,
    CUSTOM_METRIC_VALUE,
    CUSTOM_METRIC_RANGE,
    STOCK_STATUS;

    /**
     * 이름으로 조건 종류를 찾되, 없으면 null을 반환한다.
     */
    public static FilterConditionType parseOrNull(String name) {
        if (name == null) return null;
        try {
            return valueOf(name);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
