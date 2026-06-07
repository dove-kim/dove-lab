package com.dove.indicator.application.service;

import com.dove.indicator.domain.entity.StockFeatureDaily;
import com.dove.indicator.domain.entity.StockFeatureDailyId;
import com.dove.indicator.domain.enums.IndicatorType;
import com.dove.indicator.domain.repository.StockFeatureDailyRepository;
import com.dove.indicator.infrastructure.repository.StockFeatureDailyRepositorySupport;
import com.dove.stock.domain.enums.PriceType;
import com.dove.stock.domain.enums.StockExchange;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * wide 피처 행을 저장·조회하는 서비스.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class StockFeatureDailyService {

    private final StockFeatureDailyRepository repository;
    private final StockFeatureDailyRepositorySupport support;

    /**
     * wide 피처 행들을 저장한다(PK 기준 upsert — rewind 재계산 시 기존 행 갱신).
     */
    public void saveAll(List<StockFeatureDaily> features) {
        repository.saveAll(features);
    }

    /** 특정 거래일의 그룹 지표 값(없으면 빈 맵). 비감쇠 누적 지표의 시드 복원용. */
    @Transactional(readOnly = true)
    public Map<IndicatorType, Double> findIndicatorsByDate(
            String ticker, StockExchange exchange, PriceType priceType, LocalDate date) {
        return repository.findById(new StockFeatureDailyId(ticker, exchange, priceType, date))
                .map(StockFeatureDaily::toIndicatorMap)
                .orElse(Map.of());
    }

    /** 특정 날짜의 거래소·가격유형별 전 종목 지표. key = ticker. */
    @Transactional(readOnly = true)
    public Map<String, Map<IndicatorType, Double>> findAllByExchangeAndDate(
            StockExchange exchange, PriceType priceType, LocalDate date) {
        Map<String, Map<IndicatorType, Double>> result = new HashMap<>();
        for (StockFeatureDaily row : support.findByExchangeAndPriceTypeAndDate(exchange, priceType, date)) {
            result.put(row.getId().getTicker(), row.toIndicatorMap());
        }
        return result;
    }

    /** 특정 종목의 최근 N일치 지표. key = tradeDate (내림차순). */
    @Transactional(readOnly = true)
    public Map<LocalDate, Map<IndicatorType, Double>> findRecentByStock(
            String ticker, StockExchange exchange, PriceType priceType, int limit) {
        Map<LocalDate, Map<IndicatorType, Double>> result = new TreeMap<>((a, b) -> b.compareTo(a));
        for (StockFeatureDaily row : support.findRecentByTicker(ticker, exchange, priceType, limit)) {
            result.put(row.getId().getTradeDate(), row.toIndicatorMap());
        }
        return result;
    }
}
