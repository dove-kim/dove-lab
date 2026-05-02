package com.dove.stockprice.application.service;

import com.dove.market.domain.enums.MarketType;
import com.dove.stockprice.domain.entity.DailyStockPrice;
import com.dove.stockprice.domain.entity.DailyStockPriceId;
import com.dove.stockprice.domain.repository.DailyStockPriceRepository;
import com.dove.stockprice.infrastructure.repository.DailyStockPriceRepositorySupport;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class DailyStockPriceQueryService {

    private final DailyStockPriceRepository dailyStockPriceRepository;
    private final DailyStockPriceRepositorySupport dailyStockPriceQueryRepository;

    @Transactional(readOnly = true)
    public List<DailyStockPrice> findRecentDailyStockPrice(MarketType marketType, String stockCode,
                                                           LocalDate tradeDate, int limit) {
        return dailyStockPriceQueryRepository.findRecentDailyStockPrice(marketType, stockCode, tradeDate, limit);
    }

    @Transactional(readOnly = true)
    public List<String> findStockCodesByMarketTypeAndTradeDate(MarketType marketType, LocalDate tradeDate) {
        return dailyStockPriceQueryRepository.findStockCodesByMarketTypeAndTradeDate(marketType, tradeDate);
    }

    @Transactional(readOnly = true)
    public boolean existsByMarketAndCodeAndDate(MarketType marketType, String stockCode, LocalDate tradeDate) {
        return dailyStockPriceRepository.existsById(new DailyStockPriceId(marketType, stockCode, tradeDate));
    }

    @Transactional(readOnly = true)
    public boolean existsByMarketAndDate(MarketType marketType, LocalDate tradeDate) {
        return dailyStockPriceRepository.existsById_MarketTypeAndId_TradeDate(marketType, tradeDate);
    }

    @Transactional(readOnly = true)
    public Optional<LocalDate> findLatestTradeDateByMarket(MarketType marketType) {
        return dailyStockPriceRepository.findFirstById_MarketTypeOrderById_TradeDateDesc(marketType)
                .map(p -> p.getId().getTradeDate());
    }

    @Transactional(readOnly = true)
    public Set<LocalDate> findExistingTradeDatesInRange(MarketType marketType, LocalDate from, LocalDate to) {
        return new HashSet<>(dailyStockPriceQueryRepository.findDistinctTradeDatesInRange(marketType, from, to));
    }
}
