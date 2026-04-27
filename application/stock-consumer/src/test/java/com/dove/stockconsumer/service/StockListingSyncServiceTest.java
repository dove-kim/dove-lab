package com.dove.stockconsumer.service;

import com.dove.eventretry.application.service.FailedEventCommandService;
import com.dove.eventretry.application.service.PendingEventRetryCommandService;
import com.dove.market.application.service.MarketCalendarQueryService;
import com.dove.market.domain.entity.MarketCalendar;
import com.dove.market.domain.enums.MarketDayType;
import com.dove.market.domain.enums.MarketType;
import com.dove.stock.application.dto.DailyStockListingQuery;
import com.dove.stock.application.port.StockListingFetcher;
import com.dove.stock.application.service.StockCommandService;
import com.dove.stock.application.service.StockListedDateCommandService;
import com.dove.stock.application.service.StockListedDateQueryService;
import com.dove.stock.application.service.StockQueryService;
import com.dove.stock.domain.entity.Stock;
import com.dove.stock.domain.entity.StockListedDate;
import com.dove.stock.domain.enums.TradingStatus;
import com.dove.stockprice.application.service.DailyStockPriceQueryService;
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
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StockListingSyncServiceTest {

    @Mock
    private StockListingFetcher stockListingFetcher;
    @Mock
    private StockQueryService stockQueryService;
    @Mock
    private StockCommandService stockCommandService;
    @Mock
    private StockListedDateQueryService stockListedDateQueryService;
    @Mock
    private StockListedDateCommandService stockListedDateCommandService;
    @Mock
    private MarketCalendarQueryService marketCalendarQueryService;
    @Mock
    private DailyStockPriceQueryService dailyStockPriceQueryService;
    @Mock
    private PendingEventRetryCommandService pendingEventRetryCommandService;
    @Mock
    private FailedEventCommandService failedEventCommandService;

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    private final Instant fixedNow = Instant.parse("2026-04-21T00:00:00Z");
    private final Clock clock = Clock.fixed(fixedNow, ZoneOffset.UTC);

    private StockListingSyncService service;

    private final MarketType market = MarketType.KOSPI;
    private final LocalDate date = LocalDate.of(2026, 4, 17);
    private final String expectedEventKey = "listing-KOSPI-2026-04-17";

    @BeforeEach
    void setUp() {
        service = new StockListingSyncService(
                stockListingFetcher, stockQueryService, stockCommandService,
                stockListedDateQueryService, stockListedDateCommandService,
                marketCalendarQueryService, dailyStockPriceQueryService,
                pendingEventRetryCommandService, failedEventCommandService,
                objectMapper, clock);
    }

    @Test
    @DisplayName("Holiday → no-op + 재시도 큐 정리")
    void shouldBeNoOpOnHoliday() {
        when(stockListingFetcher.fetch(market, date)).thenReturn(new StockListingFetcher.Outcome.Holiday());

        service.syncForMarketAndDate(market, date);

        verify(stockCommandService, never()).saveAll(any());
        verify(stockListedDateCommandService, never()).saveAll(any());
        verify(pendingEventRetryCommandService).deleteByEvent(DailyStockListingQuery.TOPIC, expectedEventKey);
    }

    @Test
    @DisplayName("Success → Stock에 없는 코드는 ACTIVE로 INSERT + 재시도 큐 정리")
    void shouldInsertNewStocksAsActive() {
        when(stockListingFetcher.fetch(market, date))
                .thenReturn(new StockListingFetcher.Outcome.Success(Map.of("005930", "삼성전자")));
        when(stockQueryService.findAllByMarket(market)).thenReturn(List.of());
        when(stockListedDateQueryService.findAllByMarketAndDate(market, date)).thenReturn(List.of());
        when(marketCalendarQueryService.findLatestTrading(market))
                .thenReturn(Optional.of(new MarketCalendar(date, market, MarketDayType.TRADING)));
        when(dailyStockPriceQueryService.findStockCodesByMarketTypeAndTradeDate(market, date))
                .thenReturn(List.of());

        service.syncForMarketAndDate(market, date);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Stock>> captor = ArgumentCaptor.forClass(List.class);
        verify(stockCommandService).saveAll(captor.capture());
        assertThat(captor.getValue()).hasSize(1);
        assertThat(captor.getValue().get(0).getTradingStatus()).isEqualTo(TradingStatus.ACTIVE);
        verify(pendingEventRetryCommandService).deleteByEvent(DailyStockListingQuery.TOPIC, expectedEventKey);
    }

    @Test
    @DisplayName("Success → 이미 있는 StockListedDate는 skip")
    void shouldSkipExistingListedDate() {
        when(stockListingFetcher.fetch(market, date))
                .thenReturn(new StockListingFetcher.Outcome.Success(Map.of("005930", "삼성전자")));
        when(stockQueryService.findAllByMarket(market))
                .thenReturn(List.of(new Stock(market, "005930", "삼성전자", TradingStatus.ACTIVE)));
        StockListedDate existing = new StockListedDate(
                new com.dove.stock.domain.entity.StockListedDateId(market, "005930", date));
        when(stockListedDateQueryService.findAllByMarketAndDate(market, date))
                .thenReturn(List.of(existing));
        when(marketCalendarQueryService.findLatestTrading(market))
                .thenReturn(Optional.of(new MarketCalendar(date, market, MarketDayType.TRADING)));
        when(dailyStockPriceQueryService.findStockCodesByMarketTypeAndTradeDate(market, date))
                .thenReturn(List.of("005930"));

        service.syncForMarketAndDate(market, date);

        verify(stockListedDateCommandService, never()).saveAll(any());
    }

    @Test
    @DisplayName("Success + 과거 날짜 → tradingStatus 변경 없음")
    void shouldNotTouchStatusForPastDate() {
        LocalDate past = LocalDate.of(2026, 4, 10);
        Stock existing = new Stock(market, "005930", "삼성전자", TradingStatus.ACTIVE);

        when(stockListingFetcher.fetch(market, past))
                .thenReturn(new StockListingFetcher.Outcome.Success(Map.of("005930", "삼성전자")));
        when(stockQueryService.findAllByMarket(market)).thenReturn(List.of(existing));
        when(stockListedDateQueryService.findAllByMarketAndDate(market, past))
                .thenReturn(List.of());
        when(marketCalendarQueryService.findLatestTrading(market))
                .thenReturn(Optional.of(new MarketCalendar(date, market, MarketDayType.TRADING)));

        service.syncForMarketAndDate(market, past);

        assertThat(existing.getTradingStatus()).isEqualTo(TradingStatus.ACTIVE);
        verify(dailyStockPriceQueryService, never()).findStockCodesByMarketTypeAndTradeDate(any(), any());
    }

    @Test
    @DisplayName("RetryLater → PendingEventRetry enqueue (payload + reason name 포함)")
    void shouldEnqueueRetryOnRetryLater() {
        Instant nextRetry = fixedNow.plusSeconds(3600);
        when(stockListingFetcher.fetch(market, date))
                .thenReturn(new StockListingFetcher.Outcome.RetryLater(
                        StockListingFetcher.Reason.TRANSIENT, nextRetry, "503"));

        service.syncForMarketAndDate(market, date);

        verify(pendingEventRetryCommandService).enqueueOrUpdate(
                eq(DailyStockListingQuery.TOPIC),
                eq(expectedEventKey),
                any(String.class),
                eq("TRANSIENT"),
                eq(nextRetry),
                eq(fixedNow),
                eq("503"));
        verify(stockCommandService, never()).saveAll(any());
    }

    @Test
    @DisplayName("PermanentFail → FailedEvent escalate + 재시도 큐 정리")
    void shouldEscalateOnPermanentFail() {
        when(stockListingFetcher.fetch(market, date))
                .thenReturn(new StockListingFetcher.Outcome.PermanentFail(
                        StockListingFetcher.Reason.AUTH_FAILED, "401"));

        service.syncForMarketAndDate(market, date);

        verify(failedEventCommandService).escalate(
                eq(DailyStockListingQuery.TOPIC),
                eq(expectedEventKey),
                any(String.class),
                eq("AUTH_FAILED"),
                eq("401"),
                eq(fixedNow));
        verify(pendingEventRetryCommandService).deleteByEvent(DailyStockListingQuery.TOPIC, expectedEventKey);
        verify(stockCommandService, never()).saveAll(any());
    }
}
