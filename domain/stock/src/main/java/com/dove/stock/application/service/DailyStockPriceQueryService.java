package com.dove.stock.application.service;

import com.dove.market.domain.enums.MarketType;
import com.dove.stock.domain.entity.DailyStockPrice;
import com.dove.stock.domain.entity.DailyStockPriceId;
import com.dove.stock.domain.repository.DailyStockPriceRepository;
import com.dove.stock.infrastructure.repository.DailyStockPriceRepositorySupport;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
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

    @Transactional(readOnly = true)
    public Optional<LocalDate> findLatestTradeDate(List<MarketType> markets) {
        return dailyStockPriceQueryRepository.findLatestTradeDate(markets);
    }

    @Transactional(readOnly = true)
    public Optional<LocalDate> findNthRecentTradeDate(List<MarketType> markets, LocalDate reference,
                                                       boolean inclusive, long offset) {
        return dailyStockPriceQueryRepository.findNthRecentTradeDate(markets, reference, inclusive, offset);
    }

    @Transactional(readOnly = true)
    public List<LocalDate> findRecentTradeDates(List<MarketType> markets, LocalDate to, int limit) {
        return dailyStockPriceQueryRepository.findRecentTradeDates(markets, to, limit);
    }

    @Transactional(readOnly = true)
    public Map<String, DailyStockPrice> findAllByMarketsAndDate(List<MarketType> markets, LocalDate date) {
        return dailyStockPriceQueryRepository.findAllByMarketsAndDate(markets, date);
    }

    /** 시장·기간 내 종목별 거래일 집합 반환. 스캐너의 지표 누락 검사에 사용된다. */
    @Transactional(readOnly = true)
    public Map<String, Set<LocalDate>> findPriceDatesByStock(MarketType marketType, LocalDate from, LocalDate to) {
        return dailyStockPriceQueryRepository.findPriceDatesByStock(marketType, from, to);
    }

    /** 종목별 전체 주가 오름차순 조회. IndicatorBulkCalculateService 의 in-memory 슬라이딩 윈도우에 사용된다. */
    @Transactional(readOnly = true)
    public List<DailyStockPrice> findAllByMarketAndCode(MarketType marketType, String stockCode) {
        return dailyStockPriceQueryRepository.findAllByMarketAndCode(marketType, stockCode);
    }

    /** 특정 종목·날짜 목록의 주가 조회. key = tradeDate. */
    @Transactional(readOnly = true)
    public Map<LocalDate, DailyStockPrice> findByMarketAndCodeAndDates(
            MarketType marketType, String stockCode, List<LocalDate> dates) {
        return dailyStockPriceQueryRepository.findByMarketAndCodeAndDates(marketType, stockCode, dates);
    }
}
