package com.dove.indicator.application.service;

import com.dove.indicator.domain.rank.entity.StockRankDaily;
import com.dove.indicator.domain.rank.enums.RankType;
import com.dove.indicator.domain.rank.repository.StockRankDailyRepository;
import com.dove.indicator.infrastructure.repository.StockRankDailyRepositorySupport;
import com.dove.stock.domain.enums.PriceType;
import com.dove.stock.domain.enums.StockExchange;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * wide 순위 행(STOCK_RANK_DAILY)을 저장·조회하는 서비스.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class StockRankDailyService {

    private final StockRankDailyRepository repository;
    private final StockRankDailyRepositorySupport support;

    /**
     * 순위 행을 일괄 저장(upsert)한다.
     */
    public void saveAll(List<StockRankDaily> rows) {
        repository.saveAll(rows);
    }

    /**
     * 특정 날짜의 거래소·가격유형별 전 종목 순위. key = ticker.
     */
    @Transactional(readOnly = true)
    public Map<String, Map<RankType, Double>> findAllByExchangeAndDate(
            StockExchange exchange, PriceType priceType, LocalDate date) {
        Map<String, Map<RankType, Double>> result = new HashMap<>();
        for (StockRankDaily row : support.findByExchangeAndPriceTypeAndDate(exchange, priceType, date)) {
            result.put(row.getId().getTicker(), row.toRankMap());
        }
        return result;
    }
}
