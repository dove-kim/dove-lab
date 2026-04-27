package com.dove.stockconsumer.service;

import com.dove.eventretry.application.service.FailedEventCommandService;
import com.dove.eventretry.application.service.PendingEventRetryCommandService;
import com.dove.market.application.service.MarketCalendarQueryService;
import com.dove.market.domain.enums.MarketType;
import com.dove.stock.application.dto.DailyStockListingQuery;
import com.dove.stock.application.port.StockListingFetcher;
import com.dove.stock.application.service.StockCommandService;
import com.dove.stock.application.service.StockListedDateCommandService;
import com.dove.stock.application.service.StockListedDateQueryService;
import com.dove.stock.application.service.StockQueryService;
import com.dove.stock.domain.entity.Stock;
import com.dove.stock.domain.entity.StockListedDate;
import com.dove.stock.domain.entity.StockListedDateId;
import com.dove.stock.domain.enums.TradingStatus;
import com.dove.stockprice.application.service.DailyStockPriceQueryService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 주어진 (시장, 날짜)의 상장 종목 리스트를 포트로 조회하여
 * STOCK, STOCK_LISTED_DATE, Stock.tradingStatus 를 동기화한다.
 *
 * StockListingFetcher.Outcome 분기로 모든 실패 경로를 재시도 큐/DLQ에 연결:
 * - Success: 동기화 수행 + 재시도 큐 정리
 * - Holiday: no-op + 재시도 큐 정리
 * - RetryLater: PendingEventRetry 적재 (원본 payload 포함)
 * - PermanentFail: FailedEvent 승격 + 재시도 큐 정리
 *
 * 최신 개장일 동기화인 경우에만 Stock.tradingStatus 를 갱신 — 과거 날짜 동기화는 건드리지 않는다.
 */
@Service
@RequiredArgsConstructor
public class StockListingSyncService {

    private final StockListingFetcher stockListingFetcher;
    private final StockQueryService stockQueryService;
    private final StockCommandService stockCommandService;
    private final StockListedDateQueryService stockListedDateQueryService;
    private final StockListedDateCommandService stockListedDateCommandService;
    private final MarketCalendarQueryService marketCalendarQueryService;
    private final DailyStockPriceQueryService dailyStockPriceQueryService;
    private final PendingEventRetryCommandService pendingEventRetryCommandService;
    private final FailedEventCommandService failedEventCommandService;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    @Transactional
    public void syncForMarketAndDate(MarketType market, LocalDate date) {
        StockListingFetcher.Outcome outcome = stockListingFetcher.fetch(market, date);
        String eventKey = eventKey(market, date);

        switch (outcome) {
            case StockListingFetcher.Outcome.Success s -> {
                syncListedStocks(market, date, s.stocks());
                pendingEventRetryCommandService.deleteByEvent(DailyStockListingQuery.TOPIC, eventKey);
            }
            case StockListingFetcher.Outcome.Holiday h ->
                    pendingEventRetryCommandService.deleteByEvent(DailyStockListingQuery.TOPIC, eventKey);
            case StockListingFetcher.Outcome.RetryLater r -> pendingEventRetryCommandService.enqueueOrUpdate(
                    DailyStockListingQuery.TOPIC, eventKey,
                    serializePayload(new DailyStockListingQuery(date, market)),
                    r.reason().name(), r.nextRetryAt(), clock.instant(), r.detail());
            case StockListingFetcher.Outcome.PermanentFail p -> {
                failedEventCommandService.escalate(
                        DailyStockListingQuery.TOPIC, eventKey,
                        serializePayload(new DailyStockListingQuery(date, market)),
                        p.reason().name(), p.detail(), clock.instant());
                pendingEventRetryCommandService.deleteByEvent(DailyStockListingQuery.TOPIC, eventKey);
            }
        }
    }

    private void syncListedStocks(MarketType market, LocalDate date, Map<String, String> listedStocks) {
        List<Stock> existingStocks = stockQueryService.findAllByMarket(market);

        insertNewStocks(market, listedStocks, existingStocks);
        insertListedDates(market, date, listedStocks);

        updateTradingStatusIfLatestOpenDate(market, date, listedStocks, existingStocks);
    }

    private void insertNewStocks(MarketType market, Map<String, String> listedStocks, List<Stock> existingStocks) {
        Set<String> existingCodes = existingStocks.stream()
                .map(s -> s.getId().getCode())
                .collect(Collectors.toSet());

        List<Stock> toInsert = listedStocks.entrySet().stream()
                .filter(e -> !existingCodes.contains(e.getKey()))
                .map(e -> new Stock(market, e.getKey(), e.getValue(), TradingStatus.ACTIVE))
                .toList();

        if (!toInsert.isEmpty()) {
            stockCommandService.saveAll(toInsert);
        }
    }

    private void insertListedDates(MarketType market, LocalDate date, Map<String, String> listedStocks) {
        Set<String> existing = stockListedDateQueryService
                .findAllByMarketAndDate(market, date)
                .stream()
                .map(r -> r.getId().getCode())
                .collect(Collectors.toSet());

        List<StockListedDate> toInsert = listedStocks.keySet().stream()
                .filter(code -> !existing.contains(code))
                .map(code -> new StockListedDate(new StockListedDateId(market, code, date)))
                .toList();

        if (!toInsert.isEmpty()) {
            stockListedDateCommandService.saveAll(toInsert);
        }
    }

    private void updateTradingStatusIfLatestOpenDate(
            MarketType market, LocalDate date,
            Map<String, String> listedStocks, List<Stock> existingStocks) {
        if (!isLatestOpenDate(market, date)) {
            return;
        }

        Set<String> listedCodes = new HashSet<>(listedStocks.keySet());
        Set<String> tradedCodes = new HashSet<>(
                dailyStockPriceQueryService.findStockCodesByMarketTypeAndTradeDate(market, date));

        for (Stock stock : existingStocks) {
            TradingStatus next = resolveStatus(stock.getId().getCode(), listedCodes, tradedCodes);
            if (stock.getTradingStatus() != next) {
                stock.updateTradingStatus(next);
            }
        }
    }

    private TradingStatus resolveStatus(String code, Set<String> listedCodes, Set<String> tradedCodes) {
        if (!listedCodes.contains(code)) {
            return TradingStatus.DELISTED;
        }
        return tradedCodes.contains(code) ? TradingStatus.ACTIVE : TradingStatus.SUSPENDED;
    }

    private boolean isLatestOpenDate(MarketType market, LocalDate date) {
        return marketCalendarQueryService.findLatestTrading(market)
                .map(latest -> !date.isBefore(latest.getDate()))
                .orElse(true);
    }

    private String eventKey(MarketType market, LocalDate date) {
        return String.format("listing-%s-%s", market, date);
    }

    private String serializePayload(DailyStockListingQuery query) {
        try {
            return objectMapper.writeValueAsString(query);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize DailyStockListingQuery", e);
        }
    }
}
