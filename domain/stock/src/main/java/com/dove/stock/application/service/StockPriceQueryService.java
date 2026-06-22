package com.dove.stock.application.service;

import com.dove.market.domain.enums.MarketType;
import com.dove.stock.domain.entity.StockPrice;
import com.dove.stock.domain.enums.PriceType;
import com.dove.stock.domain.enums.StockExchange;
import com.dove.stock.infrastructure.repository.StockPriceRepositorySupport;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 종목 주가 조회.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StockPriceQueryService {

    private final StockPriceRepositorySupport support;

    /**
     * fromInclusive~toInclusive 구간을 거래일 오름차순으로 최대 limit개 반환한다 (지표 계산 청크용).
     */
    public List<StockPrice> findChunk(String ticker, StockExchange exchange, PriceType priceType,
                                      LocalDate fromInclusive, LocalDate toInclusive, int limit) {
        return support.findChunk(ticker, exchange, priceType, fromInclusive, toInclusive, limit);
    }

    /**
     * beforeExclusive 직전 거래일 최대 limit개를 거래일 오름차순으로 반환한다 (지표 lookback용).
     */
    public List<StockPrice> findBefore(String ticker, StockExchange exchange, PriceType priceType,
                                       LocalDate beforeExclusive, int limit) {
        List<StockPrice> desc = support.findBefore(ticker, exchange, priceType, beforeExclusive, limit);
        List<StockPrice> ascending = new ArrayList<>(desc);
        Collections.reverse(ascending);
        return ascending;
    }

    /**
     * beforeExclusive 직전까지의 거래일 수를 반환한다 (wide 피처 SEQ 부여용).
     */
    public long countBefore(String ticker, StockExchange exchange, PriceType priceType,
                            LocalDate beforeExclusive) {
        return support.countBefore(ticker, exchange, priceType, beforeExclusive);
    }

    /** 시장 집합·날짜의 전 종목 주가 (검색 필터 평가용). key=ticker. */
    public Map<String, StockPrice> findByMarketsAndDate(Collection<MarketType> markets,
                                                        PriceType priceType, LocalDate date) {
        List<StockExchange> exchanges = markets.stream().map(StockExchange::fromMarket).toList();
        return support.findByExchangesAndDate(exchanges, priceType, date).stream()
                .collect(Collectors.toMap(StockPrice::getTicker, p -> p, (a, b) -> a));
    }

    /** 시장 집합의 from~to 구간 거래일 (오름차순). */
    public List<LocalDate> findTradeDatesInRange(Collection<MarketType> markets,
                                                 PriceType priceType, LocalDate from, LocalDate to) {
        List<StockExchange> exchanges = markets.stream().map(StockExchange::fromMarket).toList();
        return support.findTradeDatesInRange(exchanges, priceType, from, to);
    }

    /** 시장 집합에서 onOrBefore 이하 offset번째(0-based) 최근 거래일. 없으면 null. */
    public LocalDate findNthRecentTradeDate(Collection<MarketType> markets,
                                            PriceType priceType, LocalDate onOrBefore, int offset) {
        List<StockExchange> exchanges = markets.stream().map(StockExchange::fromMarket).toList();
        return support.findNthRecentTradeDate(exchanges, priceType, onOrBefore, offset);
    }

    /** 거래소 집합·날짜의 전 종목 주가 (검색 필터 평가용, NXT·INTEGRATED 포함). key=ticker. */
    public Map<String, StockPrice> findByExchangesAndDate(Collection<StockExchange> exchanges,
                                                          PriceType priceType, LocalDate date) {
        return support.findByExchangesAndDate(exchanges, priceType, date).stream()
                .collect(Collectors.toMap(StockPrice::getTicker, p -> p, (a, b) -> a));
    }

    /** 거래소 집합에서 onOrBefore 이하 offset번째(0-based) 최근 거래일. 없으면 null. */
    public LocalDate findNthRecentTradeDateByExchanges(Collection<StockExchange> exchanges,
                                                       PriceType priceType, LocalDate onOrBefore, int offset) {
        return support.findNthRecentTradeDate(exchanges, priceType, onOrBefore, offset);
    }

    /** 종목+시장의 최근 N 거래일 주가 (오름차순). 차트용. */
    public List<StockPrice> findRecent(String ticker, MarketType market, PriceType priceType, int limit) {
        return findRecent(ticker, StockExchange.fromMarket(market), priceType, limit);
    }

    /** 종목+거래소의 최근 N 거래일 주가 (오름차순). NXT·INTEGRATED 포함. */
    public List<StockPrice> findRecent(String ticker, StockExchange exchange, PriceType priceType, int limit) {
        List<StockPrice> recentDesc = support.findRecentByTicker(ticker, exchange, priceType, limit);
        List<StockPrice> ascending = new ArrayList<>(recentDesc);
        Collections.reverse(ascending);
        return ascending;
    }
}
