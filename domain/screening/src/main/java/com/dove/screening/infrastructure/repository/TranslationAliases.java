package com.dove.screening.infrastructure.repository;

import com.dove.indicator.domain.entity.QStockFeatureDaily;
import com.dove.indicator.domain.rank.entity.QStockRankDaily;
import com.dove.custommetric.domain.entity.QCustomMetricDaily;
import com.dove.modelserving.domain.entity.QStockModelScore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 검색식 SQL 변환 중 누적하는 join 별칭들 — 오프셋 피처 self-join·순위·모델점수.
 */
final class TranslationAliases {

    private final Map<Integer, QStockFeatureDaily> offsetAliases = new HashMap<>();
    private final List<RankJoinAlias> rankAliases = new ArrayList<>();
    private final List<CustomMetricJoinAlias> customMetricAliases = new ArrayList<>();
    private final List<ModelScoreJoinAlias> modelScoreAliases = new ArrayList<>();

    TranslationAliases(QStockFeatureDaily base) {
        offsetAliases.put(0, base);
    }

    /**
     * 오프셋에 맞는 피처 별칭을 찾거나(없으면) 새로 만든다. 0은 기준 별칭.
     */
    QStockFeatureDaily featureAlias(int offset) {
        if (offset == 0) return offsetAliases.get(0);
        return offsetAliases.computeIfAbsent(offset,
                off -> new QStockFeatureDaily("sfdOff" + (off < 0 ? "M" : "P") + Math.abs(off)));
    }

    /**
     * 오프셋별 순위 별칭을 찾거나(없으면) 새로 만든다. 오프셋이 있으면 그 거래일을 짚을 피처 별칭도 함께 만든다.
     */
    QStockRankDaily rankAlias(int offset) {
        featureAlias(offset); // 순위 join이 붙을 오프셋 피처 행 별칭 보장
        for (RankJoinAlias r : rankAliases) {
            if (r.offset() == offset) return r.alias();
        }
        QStockRankDaily alias = new QStockRankDaily("srd" + suffix(offset));
        rankAliases.add(new RankJoinAlias(offset, alias));
        return alias;
    }

    /**
     * (오프셋, 지표) 조합별 커스텀 지표 별칭을 찾거나(없으면) 새로 만든다. 오프셋이 있으면 그 거래일을 짚을 피처 별칭도 함께 만든다.
     */
    QCustomMetricDaily customMetricAlias(int offset, long metricId) {
        featureAlias(offset); // 커스텀 지표 join이 붙을 오프셋 피처 행 별칭 보장
        for (CustomMetricJoinAlias c : customMetricAliases) {
            if (c.offset() == offset && c.metricId() == metricId) return c.alias();
        }
        QCustomMetricDaily alias = new QCustomMetricDaily("cmd" + suffix(offset) + "Metric" + metricId);
        customMetricAliases.add(new CustomMetricJoinAlias(offset, metricId, alias));
        return alias;
    }

    /**
     * (오프셋, 모델) 조합별 모델점수 별칭을 찾거나(없으면) 새로 만든다. 오프셋이 있으면 그 거래일을 짚을 피처 별칭도 함께 만든다.
     */
    QStockModelScore modelScoreAlias(int offset, long modelId) {
        featureAlias(offset); // 모델점수 join이 붙을 오프셋 피처 행 별칭 보장
        for (ModelScoreJoinAlias m : modelScoreAliases) {
            if (m.offset() == offset && m.modelId() == modelId) return m.alias();
        }
        QStockModelScore alias = new QStockModelScore("sms" + suffix(offset) + "Model" + modelId);
        modelScoreAliases.add(new ModelScoreJoinAlias(offset, modelId, alias));
        return alias;
    }

    Map<Integer, QStockFeatureDaily> offsetAliases() {
        return offsetAliases;
    }

    List<RankJoinAlias> rankAliases() {
        return rankAliases;
    }

    List<CustomMetricJoinAlias> customMetricAliases() {
        return customMetricAliases;
    }

    List<ModelScoreJoinAlias> modelScoreAliases() {
        return modelScoreAliases;
    }

    private static String suffix(int offset) {
        if (offset == 0) return "Base";
        return (offset < 0 ? "M" : "P") + Math.abs(offset);
    }
}
