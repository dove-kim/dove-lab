package com.dove.indicator.application.service;

import com.dove.indicator.domain.breadth.entity.StockBreadthDaily;
import com.dove.indicator.domain.breadth.repository.StockBreadthDailyRepository;
import com.dove.indicator.infrastructure.repository.StockBreadthDailyRepositorySupport;
import com.dove.stock.domain.enums.PriceType;
import com.dove.stock.domain.enums.StockExchange;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * 당일 상승비율 행(STOCK_BREADTH_DAILY)을 저장·조회하는 서비스.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class StockBreadthDailyService {

    private final StockBreadthDailyRepository repository;
    private final StockBreadthDailyRepositorySupport support;

    /**
     * 상승비율 행들을 저장(upsert)한다. 비어있으면 아무 것도 하지 않는다.
     */
    public void saveAll(List<StockBreadthDaily> rows) {
        repository.saveAll(rows);
    }

    /**
     * 거래소·가격유형·날짜의 당일 상승비율을 반환한다. 없으면 비어있다.
     */
    @Transactional(readOnly = true)
    public Optional<Double> findAdvanceRatio(StockExchange exchange, PriceType priceType, LocalDate date) {
        return Optional.ofNullable(support.findAdvanceRatio(exchange, priceType, date));
    }
}
