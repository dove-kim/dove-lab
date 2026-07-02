package com.dove.modelserving.domain.feature;

import com.dove.indicator.domain.entity.StockFeatureDaily;
import com.dove.indicator.domain.rank.entity.StockRankDaily;
import com.dove.indicator.domain.rank.enums.RankType;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * wide 피처 행과 wide 순위 행을 컬럼명→값 맵으로 펼치는 매퍼.
 */
@Component
public class FeatureRowMapper {

    /**
     * 피처 행의 지표값과 순위 행의 순위값을 컬럼명(대문자) 키 단일 맵으로 합친다.
     * 순위 행이 null이면 지표값만 담는다. 값이 NULL인 컬럼은 제외한다.
     */
    public Map<String, Double> toFeatureMap(StockFeatureDaily feature, StockRankDaily rank) {
        Map<String, Double> map = new HashMap<>();
        feature.toIndicatorMap().forEach((type, value) -> map.put(type.name(), value));
        if (rank != null) {
            putRank(map, RankType.RANK_RET_1D, rank.getRankRet1d());
            putRank(map, RankType.RANK_RET_5D, rank.getRankRet5d());
            putRank(map, RankType.RANK_RET_10D, rank.getRankRet10d());
            putRank(map, RankType.RANK_VOLUME_RATIO_20, rank.getRankVolumeRatio20());
            putRank(map, RankType.RANK_RSI_14, rank.getRankRsi14());
            putRank(map, RankType.RANK_MACD_HISTOGRAM, rank.getRankMacdHistogram());
            putRank(map, RankType.RANK_HIGH_52W_RATIO, rank.getRankHigh52wRatio());
            putRank(map, RankType.RANK_VOLATILITY_20D, rank.getRankVolatility20d());
            putRank(map, RankType.RANK_TURNOVER, rank.getRankTurnover());
        }
        return map;
    }

    private static void putRank(Map<String, Double> map, RankType type, Float value) {
        if (value != null) map.put(type.name(), value.doubleValue());
    }
}
