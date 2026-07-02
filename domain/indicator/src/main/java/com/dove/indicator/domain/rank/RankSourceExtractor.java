package com.dove.indicator.domain.rank;

import com.dove.indicator.domain.entity.StockFeatureDaily;
import com.dove.indicator.domain.enums.IndicatorType;
import com.dove.indicator.domain.rank.enums.RankType;

import java.util.Map;

/**
 * wide 피처 행에서 횡단면 순위의 원천 값을 꺼내는 추출기.
 */
public class RankSourceExtractor {

    /**
     * 순위 종류의 원천 값을 반환한다. TURNOVER는 거래대금 컬럼, 그 외는 동명 지표 컬럼. 없으면 null.
     */
    public Double extract(StockFeatureDaily row, RankType rankType) {
        if (rankType == RankType.RANK_TURNOVER) {
            return row.getTurnover() == null ? null : row.getTurnover().doubleValue();
        }
        IndicatorType source = rankType.sourceIndicator();
        Map<IndicatorType, Double> indicators = row.toIndicatorMap();
        return indicators.get(source);
    }
}
