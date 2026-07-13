package com.dove.modelserving.application.service;

import com.dove.indicator.domain.entity.StockFeatureDaily;
import com.dove.indicator.domain.rank.entity.StockRankDaily;
import com.dove.modelserving.domain.feature.FeatureRowMapper;
import com.dove.modelserving.domain.zone.EntryZone;
import com.dove.modelserving.infrastructure.repository.ScoreSourceRepositorySupport;
import com.dove.modelserving.infrastructure.scorer.PredictRow;
import com.dove.stock.domain.enums.PriceType;
import com.dove.stock.domain.enums.StockExchange;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 한 거래일·거래소의 진입존을 만족하는 피처 행을 채점기 입력 행으로 조립하는 조립기.
 */
@Component
@RequiredArgsConstructor
public class EntryZoneRowAssembler {

    private final ScoreSourceRepositorySupport sourceSupport;
    private final FeatureRowMapper featureRowMapper;

    /**
     * 한 거래일·거래소에서 진입존을 만족하는 행을 채점기 입력 행(피처 키 소문자)으로 만든다.
     */
    public List<PredictRow> assemble(EntryZone zone, StockExchange exchange, PriceType priceType, LocalDate date) {
        Map<String, StockFeatureDaily> features = byTicker(sourceSupport.findFeatures(exchange, priceType, date));
        Map<String, StockRankDaily> ranks = ranksByTicker(sourceSupport.findRanks(exchange, priceType, date));
        Map<String, Map<String, Double>> previous = previousFeatureMaps(exchange, priceType, date);

        List<PredictRow> inZone = new ArrayList<>();
        for (StockFeatureDaily feature : features.values()) {
            String ticker = feature.getId().getTicker();
            Map<String, Double> current = featureRowMapper.toFeatureMap(feature, ranks.get(ticker));
            Map<String, Double> prev = previous.getOrDefault(ticker, Map.of());
            if (zone.matches(current, prev)) {
                inZone.add(new PredictRow(ticker, date.toString(), toLowerKeys(current)));
            }
        }
        return inZone;
    }

    /**
     * 직전 거래일의 종목별 피처 맵을 만든다(prev_ 조건 평가용). 직전 거래일이 없으면 빈 맵.
     */
    private Map<String, Map<String, Double>> previousFeatureMaps(StockExchange exchange, PriceType priceType,
                                                                 LocalDate date) {
        LocalDate prevDate = sourceSupport.findPreviousTradeDate(exchange, priceType, date);
        if (prevDate == null) return Map.of();
        Map<String, StockRankDaily> prevRanks = ranksByTicker(sourceSupport.findRanks(exchange, priceType, prevDate));
        Map<String, Map<String, Double>> result = new HashMap<>();
        for (StockFeatureDaily feature : sourceSupport.findFeatures(exchange, priceType, prevDate)) {
            String ticker = feature.getId().getTicker();
            result.put(ticker, featureRowMapper.toFeatureMap(feature, prevRanks.get(ticker)));
        }
        return result;
    }

    private static Map<String, StockFeatureDaily> byTicker(List<StockFeatureDaily> rows) {
        Map<String, StockFeatureDaily> map = new HashMap<>();
        for (StockFeatureDaily row : rows) map.put(row.getId().getTicker(), row);
        return map;
    }

    private static Map<String, StockRankDaily> ranksByTicker(List<StockRankDaily> rows) {
        Map<String, StockRankDaily> map = new HashMap<>();
        for (StockRankDaily row : rows) map.put(row.getId().getTicker(), row);
        return map;
    }

    /**
     * 컬럼명(대문자) 키 맵을 채점기 입력용 소문자 키 맵으로 바꾼다(meta.features는 소문자).
     */
    private static Map<String, Double> toLowerKeys(Map<String, Double> upper) {
        Map<String, Double> lower = new HashMap<>(upper.size());
        upper.forEach((k, v) -> lower.put(k.toLowerCase(), v));
        return lower;
    }
}
