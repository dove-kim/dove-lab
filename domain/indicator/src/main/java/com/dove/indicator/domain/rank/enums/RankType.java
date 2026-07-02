package com.dove.indicator.domain.rank.enums;

import com.dove.indicator.domain.enums.IndicatorType;

/**
 * 그날 universe 내 횡단면 percentile 순위(0~1)의 종류와, 그 순위가 기반하는 원천 지표.
 */
public enum RankType {
    RANK_RET_1D(IndicatorType.RET_1D),
    RANK_RET_5D(IndicatorType.RET_5D),
    RANK_RET_10D(IndicatorType.RET_10D),
    RANK_VOLUME_RATIO_20(IndicatorType.VOLUME_RATIO_20),
    RANK_RSI_14(IndicatorType.RSI_14),
    RANK_MACD_HISTOGRAM(IndicatorType.MACD_HISTOGRAM),
    RANK_HIGH_52W_RATIO(IndicatorType.HIGH_52W_RATIO),
    RANK_VOLATILITY_20D(IndicatorType.VOLATILITY_20D),
    RANK_TURNOVER(null);

    private final IndicatorType sourceIndicator;

    RankType(IndicatorType sourceIndicator) {
        this.sourceIndicator = sourceIndicator;
    }

    /**
     * 이 순위가 기반하는 원천 지표를 반환한다. TURNOVER처럼 지표 컬럼이 아닌 값을 쓰면 null.
     */
    public IndicatorType sourceIndicator() {
        return sourceIndicator;
    }

    /**
     * 이름으로 RankType을 찾고, 알 수 없는 이름이면 null을 반환한다.
     */
    public static RankType parseOrNull(String name) {
        try {
            return valueOf(name);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
