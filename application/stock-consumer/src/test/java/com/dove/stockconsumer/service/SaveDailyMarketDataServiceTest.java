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
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SaveDailyMarketDataServiceTest {

    @Mock
    private DailyPriceFetcher dailyPriceFetcher;
    @Mock
    private DailyStockPriceSaveService dailyStockPriceSaveService;
    @Mock
    private MarketCalendarCommandService marketCalendarCommandService;
    @Mock
    private PendingEventRetryCommandService pendingEventRetryCommandService;
    @Mock
    private FailedEventCommandService failedEventCommandService;
    @Mock
    private StockDataChangeCommandService stockDataChangeCommandService;

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    private final Instant fixedNow = Instant.parse("2026-04-21T00:00:00Z");
    private final Clock clock = Clock.fixed(fixedNow, ZoneOffset.UTC);

    private SaveDailyMarketDataService service;

    private final LocalDate targetDate = LocalDate.of(2026, 4, 21);
    private final String expectedEventKey = "daily-KOSPI-2026-04-21";

    @BeforeEach
    void setUp() {
        service = new SaveDailyMarketDataService(
                dailyPriceFetcher, dailyStockPriceSaveService, marketCalendarCommandService,
                pendingEventRetryCommandService, failedEventCommandService,
                stockDataChangeCommandService, objectMapper, clock);
    }

    private StockInfo stockInfo(String code) {
        return new StockInfo(targetDate, MarketType.KOSPI, "삼성전자", code,
                1000L, 70000L, 71000L, 69500L, 71500L);
    }

    @Test
    @DisplayName("Success → 종목 저장 + TRADING 기록 + StockDataChange 기록 + 재시도 큐 정리")
    void shouldSaveAndMarkTradingOnSuccess() {
        when(dailyPriceFetcher.fetchDailyMarketData(MarketType.KOSPI, targetDate))
                .thenReturn(new DailyPriceFetcher.Outcome.Success(List.of(stockInfo("005930"))));

        service.saveDailyMarketDataByMarket(targetDate, MarketType.KOSPI);

        verify(dailyStockPriceSaveService).update(
                eq(targetDate), eq(MarketType.KOSPI), eq("005930"), any(),
                any(), any(), any(), any(), any());
        verify(marketCalendarCommandService).upsertDayType(targetDate, MarketType.KOSPI, MarketDayType.TRADING);

        ArgumentCaptor<StockDataChange> changeCaptor = ArgumentCaptor.forClass(StockDataChange.class);
        verify(stockDataChangeCommandService).save(changeCaptor.capture());
        assertThat(changeCaptor.getValue().getId().getStockCode()).isEqualTo("005930");

        verify(pendingEventRetryCommandService).deleteByEvent(DailyStockPriceQuery.TOPIC, expectedEventKey);
    }

    @Test
    @DisplayName("Holiday → HOLIDAY 기록 + 재시도 큐 정리 + 종목 저장 없음")
    void shouldMarkHolidayAndSaveNothingOnHoliday() {
        when(dailyPriceFetcher.fetchDailyMarketData(MarketType.KOSPI, targetDate))
                .thenReturn(new DailyPriceFetcher.Outcome.Holiday());

        service.saveDailyMarketDataByMarket(targetDate, MarketType.KOSPI);

        verify(marketCalendarCommandService).upsertDayType(targetDate, MarketType.KOSPI, MarketDayType.HOLIDAY);
        verify(dailyStockPriceSaveService, never()).update(any(), any(), any(), any(), any(), any(), any(), any(), any());
        verify(stockDataChangeCommandService, never()).save(any());
        verify(pendingEventRetryCommandService).deleteByEvent(DailyStockPriceQuery.TOPIC, expectedEventKey);
    }

    @Test
    @DisplayName("RetryLater → PendingEventRetry enqueue (payload + reason name 포함)")
    void shouldEnqueueRetryOnRetryLater() {
        Instant nextRetry = fixedNow.plusSeconds(3600);
        when(dailyPriceFetcher.fetchDailyMarketData(MarketType.KOSPI, targetDate))
                .thenReturn(new DailyPriceFetcher.Outcome.RetryLater(
                        DailyPriceFetcher.Reason.UNCERTAIN, nextRetry, "not yet"));

        service.saveDailyMarketDataByMarket(targetDate, MarketType.KOSPI);

        verify(pendingEventRetryCommandService).enqueueOrUpdate(
                eq(DailyStockPriceQuery.TOPIC),
                eq(expectedEventKey),
                any(String.class),
                eq("UNCERTAIN"),
                eq(nextRetry),
                eq(fixedNow),
                eq("not yet"));
        verify(marketCalendarCommandService, never()).upsertDayType(any(), any(), any());
    }

    @Test
    @DisplayName("PermanentFail → FailedEvent escalate + 재시도 큐 정리")
    void shouldEscalateOnPermanentFail() {
        when(dailyPriceFetcher.fetchDailyMarketData(MarketType.KOSPI, targetDate))
                .thenReturn(new DailyPriceFetcher.Outcome.PermanentFail(
                        DailyPriceFetcher.Reason.AUTH_FAILED, "401"));

        service.saveDailyMarketDataByMarket(targetDate, MarketType.KOSPI);

        verify(failedEventCommandService).escalate(
                eq(DailyStockPriceQuery.TOPIC),
                eq(expectedEventKey),
                any(String.class),
                eq("AUTH_FAILED"),
                eq("401"),
                eq(fixedNow));
        verify(pendingEventRetryCommandService).deleteByEvent(DailyStockPriceQuery.TOPIC, expectedEventKey);
    }
}
