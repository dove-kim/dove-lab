package com.dove.stockconsumer.service;

import com.dove.eventretry.application.service.FailedEventCommandService;
import com.dove.eventretry.application.service.PendingEventRetryCommandService;
import com.dove.market.application.service.MarketCalendarCommandService;
import com.dove.market.domain.enums.MarketDayType;
import com.dove.market.domain.enums.MarketType;
import com.dove.stockprice.application.dto.DailyStockPriceQuery;
import com.dove.stockprice.application.port.DailyPriceFetcher;
import com.dove.stockprice.application.port.StockInfo;
import com.dove.stockprice.application.service.StockDataChangeCommandService;
import com.dove.stockprice.domain.entity.StockDataChange;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;

/** DailyPriceFetcher.Outcome 분기: 저장 / HOLIDAY 기록 / 재조회 예약 / 관리자 개입. */
@Service
@RequiredArgsConstructor
public class SaveDailyMarketDataService {
    private final DailyPriceFetcher dailyPriceFetcher;
    private final DailyStockPriceSaveService dailyStockPriceSaveService;
    private final MarketCalendarCommandService marketCalendarCommandService;

    private final PendingEventRetryCommandService pendingEventRetryCommandService;
    private final FailedEventCommandService failedEventCommandService;

    private final StockDataChangeCommandService stockDataChangeCommandService;

    private final ObjectMapper objectMapper;
    private final Clock clock;

    @Transactional
    public void saveDailyMarketDataByMarket(LocalDate targetDate, MarketType marketType) {
        DailyPriceFetcher.Outcome outcome = dailyPriceFetcher.fetchDailyMarketData(marketType, targetDate);
        String eventKey = eventKey(marketType, targetDate);

        switch (outcome) {
            case DailyPriceFetcher.Outcome.Success s -> {
                for (StockInfo info : s.stocks()) {
                    saveStockInfo(info);
                    stockDataChangeCommandService.save(
                            new StockDataChange(marketType, info.stockCode(), targetDate));
                }
                marketCalendarCommandService.upsertDayType(targetDate, marketType, MarketDayType.TRADING);
                pendingEventRetryCommandService.deleteByEvent(DailyStockPriceQuery.TOPIC, eventKey);
            }
            case DailyPriceFetcher.Outcome.Holiday h -> {
                marketCalendarCommandService.upsertDayType(targetDate, marketType, MarketDayType.HOLIDAY);
                pendingEventRetryCommandService.deleteByEvent(DailyStockPriceQuery.TOPIC, eventKey);
            }
            case DailyPriceFetcher.Outcome.RetryLater r -> pendingEventRetryCommandService.enqueueOrUpdate(
                    DailyStockPriceQuery.TOPIC, eventKey,
                    serializePayload(new DailyStockPriceQuery(targetDate, marketType)),
                    r.reason().name(), r.nextRetryAt(), clock.instant(), r.detail());
            case DailyPriceFetcher.Outcome.PermanentFail p -> {
                failedEventCommandService.escalate(
                        DailyStockPriceQuery.TOPIC, eventKey,
                        serializePayload(new DailyStockPriceQuery(targetDate, marketType)),
                        p.reason().name(), p.detail(), clock.instant());
                pendingEventRetryCommandService.deleteByEvent(DailyStockPriceQuery.TOPIC, eventKey);
            }
        }
    }

    private void saveStockInfo(StockInfo info) {
        dailyStockPriceSaveService.update(
                info.tradingDate(), info.marketType(), info.stockCode(), info.stockName(),
                info.tradingVolume(),
                info.openingPrice(), info.closingPrice(),
                info.lowestPrice(), info.highestPrice()
        );
    }

    private String eventKey(MarketType marketType, LocalDate targetDate) {
        return String.format("daily-%s-%s", marketType, targetDate);
    }

    private String serializePayload(DailyStockPriceQuery query) {
        try {
            return objectMapper.writeValueAsString(query);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize DailyStockPriceQuery", e);
        }
    }
}
