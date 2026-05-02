package com.dove.stockconsumer.service;

import com.dove.eventretry.application.service.FailedEventCommandService;
import com.dove.market.application.service.MarketDataCursorCommandService;
import com.dove.market.application.service.MarketTradingDateCommandService;
import com.dove.market.domain.enums.MarketType;
import com.dove.stock.application.port.StockListingFetcher;
import com.dove.stock.application.service.StockCommandService;
import com.dove.stock.application.service.StockListedDateCommandService;
import com.dove.stock.application.service.StockListedDateQueryService;
import com.dove.stock.application.service.StockQueryService;
import com.dove.stock.domain.entity.Stock;
import com.dove.stock.domain.entity.StockListedDate;
import com.dove.stock.domain.entity.StockListedDateId;
import com.dove.stock.domain.enums.TradingStatus;
import com.dove.market.application.dto.DailyMarketDataQuery;
import com.dove.stockprice.application.port.DailyPriceFetcher;
import com.dove.stockprice.application.port.StockInfo;
import com.dove.technicalindicator.application.dto.IndicatorCalcTrigger;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.ArgumentCaptor;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DailyMarketDataServiceTest {

    @Mock private StockListingFetcher stockListingFetcher;
    @Mock private DailyPriceFetcher dailyPriceFetcher;
    @Mock private StockQueryService stockQueryService;
    @Mock private StockCommandService stockCommandService;
    @Mock private StockListedDateQueryService stockListedDateQueryService;
    @Mock private StockListedDateCommandService stockListedDateCommandService;
    @Mock private DailyStockPriceSaveService dailyStockPriceSaveService;
    @Mock private MarketDataCursorCommandService marketDataCursorCommandService;
    @Mock private MarketTradingDateCommandService marketTradingDateCommandService;
    @Mock private FailedEventCommandService failedEventCommandService;
    @Mock private KafkaTemplate<String, Object> kafkaTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    private final Instant fixedNow = Instant.parse("2026-04-21T00:00:00Z");
    private final Clock clock = Clock.fixed(fixedNow, ZoneOffset.UTC);

    private DailyMarketDataService service;

    private final MarketType market = MarketType.KOSPI;
    private final LocalDate date = LocalDate.of(2026, 4, 17);

    @BeforeEach
    void setUp() {
        service = new DailyMarketDataService(
                stockListingFetcher, dailyPriceFetcher,
                stockQueryService, stockCommandService,
                stockListedDateQueryService, stockListedDateCommandService,
                dailyStockPriceSaveService, marketDataCursorCommandService,
                marketTradingDateCommandService, failedEventCommandService,
                kafkaTemplate, objectMapper, clock);
    }

    private StockInfo stockInfo(String code) {
        return new StockInfo(date, market, code, 1000L, 70000L, 71000L, 69500L, 71500L);
    }

    // ── Holiday / 휴장일 ────────────────────────────────────────────────

    @Test
    @DisplayName("Holiday(listing) + Holiday(price) → 커서 전진, isOpen=false 저장, 지표 없음")
    void shouldAdvanceCursorOnHoliday() {
        when(stockListingFetcher.fetch(market, date)).thenReturn(new StockListingFetcher.Outcome.Holiday());
        when(dailyPriceFetcher.fetchDailyMarketData(market, date)).thenReturn(new DailyPriceFetcher.Outcome.Holiday());

        service.process(market, date);

        verify(marketDataCursorCommandService).upsert(market, date);
        verify(marketTradingDateCommandService).upsert(market, date, false);
        verify(stockCommandService, never()).saveAll(any());
        verify(dailyStockPriceSaveService, never()).save(any(), any(), any(), any(), any(), any(), any(), any());
        verify(kafkaTemplate, never()).send(any(), any(), any());
    }

    @Test
    @DisplayName("Success(listing) + Holiday(price) → 종목·커서 저장, isOpen=false 저장, 지표 없음")
    void shouldSaveListingButNoIndicatorOnHoliday() {
        when(stockListingFetcher.fetch(market, date))
                .thenReturn(new StockListingFetcher.Outcome.Success(Map.of("005930", "삼성전자")));
        when(dailyPriceFetcher.fetchDailyMarketData(market, date)).thenReturn(new DailyPriceFetcher.Outcome.Holiday());
        when(stockQueryService.findAllByMarket(market)).thenReturn(List.of());
        when(stockListedDateQueryService.findAllByMarketAndDate(market, date)).thenReturn(List.of());

        service.process(market, date);

        verify(stockListedDateCommandService).saveAll(any());
        verify(marketDataCursorCommandService).upsert(market, date);
        verify(marketTradingDateCommandService).upsert(market, date, false);
        verify(kafkaTemplate, never()).send(any(), any(), any());
    }

    // ── RetryLater ────────────────────────────────────────────────────

    @Test
    @DisplayName("RetryLater(listing) → 예외 throw, 커서 미전진, MarketTradingDate 미저장")
    void shouldThrowOnListingRetryLater() {
        when(stockListingFetcher.fetch(market, date))
                .thenReturn(new StockListingFetcher.Outcome.RetryLater(
                        StockListingFetcher.Reason.TRANSIENT, fixedNow.plusSeconds(3600), "503"));

        assertThatThrownBy(() -> service.process(market, date))
                .isInstanceOf(IllegalStateException.class);

        verify(marketDataCursorCommandService, never()).upsert(any(), any());
        verify(marketTradingDateCommandService, never()).upsert(any(), any(), anyBoolean());
    }

    @Test
    @DisplayName("RetryLater(price) → 예외 throw, 커서 미전진, MarketTradingDate 미저장")
    void shouldThrowOnPriceRetryLater() {
        when(stockListingFetcher.fetch(market, date)).thenReturn(new StockListingFetcher.Outcome.Holiday());
        when(dailyPriceFetcher.fetchDailyMarketData(market, date))
                .thenReturn(new DailyPriceFetcher.Outcome.RetryLater(
                        DailyPriceFetcher.Reason.TRANSIENT, fixedNow.plusSeconds(3600), "503"));

        assertThatThrownBy(() -> service.process(market, date))
                .isInstanceOf(IllegalStateException.class);

        verify(marketDataCursorCommandService, never()).upsert(any(), any());
        verify(marketTradingDateCommandService, never()).upsert(any(), any(), anyBoolean());
    }

    // ── PermanentFail ─────────────────────────────────────────────────

    @Test
    @DisplayName("PermanentFail(listing) → escalate + 커서 전진, MarketTradingDate 미저장")
    void shouldEscalateAndAdvanceCursorOnListingPermanentFail() {
        when(stockListingFetcher.fetch(market, date))
                .thenReturn(new StockListingFetcher.Outcome.PermanentFail(
                        StockListingFetcher.Reason.AUTH_FAILED, "401"));

        service.process(market, date);

        verify(failedEventCommandService).escalate(
                eq(DailyMarketDataQuery.TOPIC), eq("KOSPI-2026-04-17"),
                any(String.class), eq("AUTH_FAILED"), eq("401"), eq(fixedNow));
        verify(marketDataCursorCommandService).upsert(market, date);
        verify(marketTradingDateCommandService, never()).upsert(any(), any(), anyBoolean());
        verify(dailyStockPriceSaveService, never()).save(any(), any(), any(), any(), any(), any(), any(), any());
    }

    // ── 개장일 정상 처리 ──────────────────────────────────────────────

    @Test
    @DisplayName("개장일 — 신규 종목 ACTIVE로 저장, 주가 저장, isOpen=true, 커서 전진")
    void shouldSaveNewStockAndPriceOnOpenDay() {
        when(stockListingFetcher.fetch(market, date))
                .thenReturn(new StockListingFetcher.Outcome.Success(Map.of("005930", "삼성전자")));
        when(dailyPriceFetcher.fetchDailyMarketData(market, date))
                .thenReturn(new DailyPriceFetcher.Outcome.Success(List.of(stockInfo("005930"))));
        when(stockQueryService.findAllByMarket(market)).thenReturn(List.of());
        when(stockListedDateQueryService.findAllByMarketAndDate(market, date)).thenReturn(List.of());

        service.process(market, date);

        verify(stockCommandService).saveAll(argThat(list ->
                list.size() == 1 && ((Stock) list.get(0)).getTradingStatus() == TradingStatus.ACTIVE));
        verify(dailyStockPriceSaveService).save(eq(date), eq(market), eq("005930"),
                any(), any(), any(), any(), any());
        verify(marketDataCursorCommandService).upsert(market, date);
        verify(marketTradingDateCommandService).upsert(market, date, true);
    }

    @Test
    @DisplayName("개장일 — 거래된 종목에 대해 INDICATOR_CALC_TRIGGER 발행, insertedDate = 처리 날짜")
    void shouldPublishIndicatorCalcTriggerForTradedStocks() {
        when(stockListingFetcher.fetch(market, date))
                .thenReturn(new StockListingFetcher.Outcome.Success(Map.of("005930", "삼성전자")));
        when(dailyPriceFetcher.fetchDailyMarketData(market, date))
                .thenReturn(new DailyPriceFetcher.Outcome.Success(List.of(stockInfo("005930"))));
        when(stockQueryService.findAllByMarket(market)).thenReturn(
                List.of(new Stock(market, "005930", "삼성전자", TradingStatus.ACTIVE)));
        when(stockListedDateQueryService.findAllByMarketAndDate(market, date)).thenReturn(List.of());

        service.process(market, date);

        ArgumentCaptor<IndicatorCalcTrigger> captor = ArgumentCaptor.forClass(IndicatorCalcTrigger.class);
        verify(kafkaTemplate).send(eq(IndicatorCalcTrigger.TOPIC), eq("KOSPI-005930"), captor.capture());
        assertThat(captor.getValue().getInsertedDate()).isEqualTo(date);
    }

    @Test
    @DisplayName("개장일 — 거래 안 된 종목(SUSPENDED)은 트리거 대상 제외, 거래된 종목만 발행")
    void shouldOnlyPublishForTradedStocksNotSuspended() {
        // 005930 거래 있음, 000660 종목 목록에는 있지만 주가 없음(SUSPENDED)
        Stock suspendedStock = new Stock(market, "000660", "SK하이닉스", TradingStatus.ACTIVE);
        when(stockListingFetcher.fetch(market, date))
                .thenReturn(new StockListingFetcher.Outcome.Success(
                        Map.of("000660", "SK하이닉스", "005930", "삼성전자")));
        when(dailyPriceFetcher.fetchDailyMarketData(market, date))
                .thenReturn(new DailyPriceFetcher.Outcome.Success(List.of(stockInfo("005930"))));
        when(stockQueryService.findAllByMarket(market)).thenReturn(List.of(suspendedStock));
        when(stockListedDateQueryService.findAllByMarketAndDate(market, date))
                .thenReturn(List.of(new StockListedDate(new StockListedDateId(market, "000660", date))));

        service.process(market, date);

        verify(kafkaTemplate).send(eq(IndicatorCalcTrigger.TOPIC), eq("KOSPI-005930"), any(IndicatorCalcTrigger.class));
        verify(kafkaTemplate, never()).send(eq(IndicatorCalcTrigger.TOPIC), eq("KOSPI-000660"), any());
    }

    @Test
    @DisplayName("개장일 — 상태 변경 없는 기존 ACTIVE 종목도 거래됐으면 트리거 발행")
    void shouldPublishForTradedStockEvenWithNoStatusChange() {
        Stock stock = new Stock(market, "005930", "삼성전자", TradingStatus.ACTIVE);
        when(stockListingFetcher.fetch(market, date))
                .thenReturn(new StockListingFetcher.Outcome.Success(Map.of("005930", "삼성전자")));
        when(dailyPriceFetcher.fetchDailyMarketData(market, date))
                .thenReturn(new DailyPriceFetcher.Outcome.Success(List.of(stockInfo("005930"))));
        when(stockQueryService.findAllByMarket(market)).thenReturn(List.of(stock));
        when(stockListedDateQueryService.findAllByMarketAndDate(market, date))
                .thenReturn(List.of(new StockListedDate(new StockListedDateId(market, "005930", date))));

        service.process(market, date);

        verify(kafkaTemplate).send(eq(IndicatorCalcTrigger.TOPIC), eq("KOSPI-005930"), any(IndicatorCalcTrigger.class));
    }
}
