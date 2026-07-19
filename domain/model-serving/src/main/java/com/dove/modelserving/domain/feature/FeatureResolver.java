package com.dove.modelserving.domain.feature;

import com.dove.indicator.domain.enums.IndicatorType;
import com.dove.indicator.domain.rank.enums.RankType;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * 모델 피처 이름을 저장 테이블·컬럼으로 해석하는 피처 레지스트리.
 */
@Component
public class FeatureResolver {

    /**
     * 피처 이름을 (테이블, 컬럼)으로 해석한다. 이름은 대소문자 무관(내부에서 대문자화).
     * IndicatorType이면 STOCK_FEATURE_DAILY, RankType이면 STOCK_RANK_DAILY,
     * 원시 시세 컬럼(RawFeature: VOLUME·TURNOVER)이면 STOCK_FEATURE_DAILY로 해석하고,
     * 모두 아니면 미해석으로 빈 Optional을 반환한다.
     */
    public Optional<FeatureSource> resolve(String name) {
        if (name == null) return Optional.empty();
        String upper = name.toUpperCase();

        IndicatorType indicator = IndicatorType.parseOrNull(upper);
        if (indicator != null) {
            return Optional.of(new FeatureSource(FeatureTable.STOCK_FEATURE_DAILY, indicator.name()));
        }

        RankType rank = RankType.parseOrNull(upper);
        if (rank != null) {
            return Optional.of(new FeatureSource(FeatureTable.STOCK_RANK_DAILY, rank.name()));
        }

        RawFeature raw = RawFeature.parseOrNull(upper);
        if (raw != null) {
            return Optional.of(new FeatureSource(FeatureTable.STOCK_FEATURE_DAILY, raw.name()));
        }

        return Optional.empty();
    }

    /**
     * 피처 이름이 레지스트리로 해석되는지 여부.
     */
    public boolean isResolvable(String name) {
        return resolve(name).isPresent();
    }
}
