package com.dove.stockprice.application.service;

import com.dove.market.domain.enums.MarketType;
import com.dove.stockprice.domain.entity.DailyStockPrice;
import com.dove.stockprice.infrastructure.repository.DailyStockPriceRepositorySupport;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/** 주가 데이터 조회 전용 서비스. */
@Service
@RequiredArgsConstructor
public class DailyStockPriceQueryService {

    private final DailyStockPriceRepositorySupport dailyStockPriceQueryRepository;

    @Transactional(readOnly = true)
    public List<LocalDate> findTradeDatesFrom(MarketType marketType, String stockCode, LocalDate fromDate) {
        return dailyStockPriceQueryRepository.findTradeDatesFrom(marketType, stockCode, fromDate);
    }

    @Transactional(readOnly = true)
    public List<LocalDate> findTradeDatesAfter(MarketType marketType, String stockCode, LocalDate tradeDate) {
        return dailyStockPriceQueryRepository.findTradeDatesAfter(marketType, stockCode, tradeDate);
    }

    @Transactional(readOnly = true)
    public List<DailyStockPrice> findRecentDailyStockPrice(MarketType marketType, String stockCode,
                                                           LocalDate tradeDate, int limit) {
        return dailyStockPriceQueryRepository.findRecentDailyStockPrice(marketType, stockCode, tradeDate, limit);
    }

    @Transactional(readOnly = true)
    public List<String> findStockCodesByMarketTypeAndTradeDate(MarketType marketType, LocalDate tradeDate) {
        return dailyStockPriceQueryRepository.findStockCodesByMarketTypeAndTradeDate(marketType, tradeDate);
    }
}
