package com.dove.scheduler.service;

import com.dove.market.domain.enums.MarketType;
import com.dove.stock.application.service.DailyStockPriceQueryService;
import com.dove.stock.domain.entity.DailyStockPrice;
import com.dove.indicator.application.service.TechnicalIndicatorCommandService;
import com.dove.indicator.application.service.TechnicalIndicatorQueryService;
import com.dove.indicator.domain.calculator.TechnicalIndicatorCalculator;
import com.dove.indicator.domain.enums.IndicatorType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class IndicatorDailyCalculateServiceTest {

    @Mock DailyStockPriceQueryService dailyStockPriceQueryService;
    @Mock TechnicalIndicatorQueryService technicalIndicatorQueryService;
    @Mock TechnicalIndicatorCommandService technicalIndicatorCommandService;
    @Mock TechnicalIndicatorCalculator calculator;

    IndicatorDailyCalculateService service;

    static final MarketType MARKET = MarketType.KOSPI;
    static final LocalDate DATE = LocalDate.of(2026, 4, 17);
    static final LocalDate PREV_DATE = LocalDate.of(2026, 4, 16);
    static final String CODE = "005930";

    @BeforeEach
    void setUp() {
        // 서비스 생성자에서만 사용 → strict stub 오탐 방지를 위해 lenient 설정
        lenient().when(calculator.requiredDataSize()).thenReturn(5);
        lenient().when(calculator.isCumulative()).thenReturn(false);
        lenient().when(calculator.indicatorType()).thenReturn(IndicatorType.SMA_5);

        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(2);
        executor.initialize();

        service = new IndicatorDailyCalculateService(
                dailyStockPriceQueryService, technicalIndicatorQueryService,
                technicalIndicatorCommandService, List.of(calculator), executor);
    }

    @Test
    @DisplayName("해당 날짜 종목 없음 → 계산 없음")
    void shouldDoNothingWhenNoStocksForDate() {
        when(dailyStockPriceQueryService.findStockCodesByMarketTypeAndTradeDate(MARKET, DATE))
                .thenReturn(List.of());

        service.calculateAndSave(MARKET, DATE);

        verify(technicalIndicatorCommandService, never()).saveAll(any());
    }

    @Test
    @DisplayName("주가 데이터 없는 종목 → 계산 건너뜀")
    void shouldSkipStockWhenNoPriceData() {
        when(dailyStockPriceQueryService.findStockCodesByMarketTypeAndTradeDate(MARKET, DATE))
                .thenReturn(List.of(CODE));
        when(dailyStockPriceQueryService.findRecentDailyStockPrice(MARKET, CODE, DATE, 5))
                .thenReturn(List.of());

        service.calculateAndSave(MARKET, DATE);

        verify(technicalIndicatorCommandService, never()).saveAll(any());
    }

    @Test
    @DisplayName("데이터 부족(requiredSize 미달) → 해당 계산기 건너뜀")
    void shouldSkipCalculatorWhenInsufficientData() {
        when(dailyStockPriceQueryService.findStockCodesByMarketTypeAndTradeDate(MARKET, DATE))
                .thenReturn(List.of(CODE));
        when(dailyStockPriceQueryService.findRecentDailyStockPrice(MARKET, CODE, DATE, 5))
                .thenReturn(samplePrices(3));

        service.calculateAndSave(MARKET, DATE);

        verify(calculator, never()).calculate(any());
        verify(technicalIndicatorCommandService, never()).saveAll(any());
    }

    @Test
    @DisplayName("비누적 지표 — calculate() 호출 후 결과 저장")
    void shouldCalculateNonCumulativeIndicator() {
        when(dailyStockPriceQueryService.findStockCodesByMarketTypeAndTradeDate(MARKET, DATE))
                .thenReturn(List.of(CODE));
        when(dailyStockPriceQueryService.findRecentDailyStockPrice(MARKET, CODE, DATE, 5))
                .thenReturn(samplePrices(5));
        when(calculator.calculate(any())).thenReturn(Map.of(IndicatorType.SMA_5, 60_000.0));

        service.calculateAndSave(MARKET, DATE);

        verify(calculator).calculate(any());
        verify(calculator, never()).calculateWithSeed(any(), anyDouble());
        ArgumentCaptor<List> captor = ArgumentCaptor.forClass(List.class);
        verify(technicalIndicatorCommandService).saveAll(captor.capture());
        assertThat(captor.getValue()).hasSize(1);
    }

    @Test
    @DisplayName("누적 지표 — 전날 벌크 시드 조회 후 calculateWithSeed() 호출")
    void shouldCalculateCumulativeIndicatorWithBulkSeed() {
        when(calculator.isCumulative()).thenReturn(true);
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(2);
        executor.initialize();
        service = new IndicatorDailyCalculateService(
                dailyStockPriceQueryService, technicalIndicatorQueryService,
                technicalIndicatorCommandService, List.of(calculator), executor);

        when(dailyStockPriceQueryService.findStockCodesByMarketTypeAndTradeDate(MARKET, DATE))
                .thenReturn(List.of(CODE));
        when(dailyStockPriceQueryService.findNthRecentTradeDate(List.of(MARKET), DATE, false, 0))
                .thenReturn(Optional.of(PREV_DATE));
        when(technicalIndicatorQueryService.findSeedsByMarketAndDate(
                eq(MARKET), eq(PREV_DATE), eq(Set.of(IndicatorType.SMA_5))))
                .thenReturn(Map.of(CODE, Map.of(IndicatorType.SMA_5, 58_000.0)));
        when(dailyStockPriceQueryService.findRecentDailyStockPrice(MARKET, CODE, DATE, 5))
                .thenReturn(samplePrices(5));
        when(calculator.calculateWithSeed(any(), eq(58_000.0)))
                .thenReturn(Map.of(IndicatorType.SMA_5, 59_000.0));

        service.calculateAndSave(MARKET, DATE);

        verify(calculator).calculateWithSeed(any(), eq(58_000.0));
        verify(calculator, never()).calculate(any());
        verify(technicalIndicatorCommandService).saveAll(any());
    }

    @Test
    @DisplayName("누적 지표 — 이전 거래일 없음(최초 실행) → SMA 초기화로 calculate() 호출")
    void shouldUseSmaInitWhenNoPrevDateExists() {
        when(calculator.isCumulative()).thenReturn(true);
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(2);
        executor.initialize();
        service = new IndicatorDailyCalculateService(
                dailyStockPriceQueryService, technicalIndicatorQueryService,
                technicalIndicatorCommandService, List.of(calculator), executor);

        when(dailyStockPriceQueryService.findStockCodesByMarketTypeAndTradeDate(MARKET, DATE))
                .thenReturn(List.of(CODE));
        when(dailyStockPriceQueryService.findNthRecentTradeDate(List.of(MARKET), DATE, false, 0))
                .thenReturn(Optional.empty());
        when(dailyStockPriceQueryService.findRecentDailyStockPrice(MARKET, CODE, DATE, 5))
                .thenReturn(samplePrices(5));
        when(calculator.calculate(any())).thenReturn(Map.of(IndicatorType.SMA_5, 60_000.0));

        service.calculateAndSave(MARKET, DATE);

        verify(calculator).calculate(any());
        verify(calculator, never()).calculateWithSeed(any(), anyDouble());
        verify(technicalIndicatorQueryService, never()).findSeedsByMarketAndDate(any(), any(), any());
    }

    @Test
    @DisplayName("여러 종목 — 각 종목별로 독립 계산")
    void shouldCalculateEachStockIndependently() {
        String code2 = "000660";
        when(dailyStockPriceQueryService.findStockCodesByMarketTypeAndTradeDate(MARKET, DATE))
                .thenReturn(List.of(CODE, code2));
        when(dailyStockPriceQueryService.findRecentDailyStockPrice(eq(MARKET), any(), eq(DATE), eq(5)))
                .thenReturn(samplePrices(5));
        when(calculator.calculate(any())).thenReturn(Map.of(IndicatorType.SMA_5, 60_000.0));

        service.calculateAndSave(MARKET, DATE);

        verify(calculator, times(2)).calculate(any());
        verify(technicalIndicatorCommandService, times(2)).saveAll(any());
    }

    @Test
    @DisplayName("calculateRange — 구간 내 거래일 없음 → 계산 없음")
    void shouldDoNothingWhenNoTradeDatesInRange() {
        LocalDate from = LocalDate.of(2026, 4, 14);
        LocalDate upTo = LocalDate.of(2026, 4, 17);
        when(dailyStockPriceQueryService.findExistingTradeDatesInRange(MARKET, from, upTo))
                .thenReturn(Set.of());

        service.calculateRange(MARKET, from, upTo);

        verify(dailyStockPriceQueryService, never())
                .findStockCodesByMarketTypeAndTradeDate(any(), any());
    }

    @Test
    @DisplayName("calculateRange — 모두 미계산 → 거래일 수만큼 findStockCodes 호출")
    void shouldCallCalculateAndSaveForEachUncalculatedDate() {
        LocalDate d1 = LocalDate.of(2026, 4, 14);
        LocalDate d2 = LocalDate.of(2026, 4, 16);
        LocalDate d3 = LocalDate.of(2026, 4, 17);
        when(dailyStockPriceQueryService.findExistingTradeDatesInRange(MARKET, d1, d3))
                .thenReturn(new TreeSet<>(Set.of(d1, d2, d3)));
        when(technicalIndicatorQueryService.findCalculatedDatesPerStock(
                eq(MARKET), eq(IndicatorType.SMA_5), eq(d1), eq(d3)))
                .thenReturn(Map.of()); // 아무것도 계산 안 됨
        when(dailyStockPriceQueryService.findStockCodesByMarketTypeAndTradeDate(eq(MARKET), any()))
                .thenReturn(List.of());

        service.calculateRange(MARKET, d1, d3);

        verify(dailyStockPriceQueryService, times(3))
                .findStockCodesByMarketTypeAndTradeDate(eq(MARKET), any());
    }

    @Test
    @DisplayName("calculateRange — 이미 계산된 종목은 건너뜀")
    void shouldSkipAlreadyCalculatedStocks() {
        LocalDate d1 = LocalDate.of(2026, 4, 14);
        LocalDate d2 = LocalDate.of(2026, 4, 16);
        when(dailyStockPriceQueryService.findExistingTradeDatesInRange(MARKET, d1, d2))
                .thenReturn(new TreeSet<>(Set.of(d1, d2)));
        // d2의 CODE 종목은 이미 계산됨, d1은 미계산
        when(technicalIndicatorQueryService.findCalculatedDatesPerStock(
                eq(MARKET), eq(IndicatorType.SMA_5), eq(d1), eq(d2)))
                .thenReturn(Map.of(CODE, Set.of(d2)));
        when(dailyStockPriceQueryService.findStockCodesByMarketTypeAndTradeDate(eq(MARKET), any()))
                .thenReturn(List.of(CODE));
        // d1의 CODE는 targets에 포함 → processStock 진입 → 주가 없음(빈 목록) → 계산 건너뜀
        when(dailyStockPriceQueryService.findRecentDailyStockPrice(MARKET, CODE, d1, 5))
                .thenReturn(List.of());

        service.calculateRange(MARKET, d1, d2);

        // d1, d2 두 날짜 모두 findStockCodes 호출
        verify(dailyStockPriceQueryService, times(2))
                .findStockCodesByMarketTypeAndTradeDate(eq(MARKET), any());
        // d1의 CODE는 미계산 → findRecentDailyStockPrice 호출됨
        verify(dailyStockPriceQueryService).findRecentDailyStockPrice(MARKET, CODE, d1, 5);
        // d2의 CODE는 이미 계산됨 → findRecentDailyStockPrice 호출 없음
        verify(dailyStockPriceQueryService, never()).findRecentDailyStockPrice(eq(MARKET), eq(CODE), eq(d2), anyInt());
    }

    private List<DailyStockPrice> samplePrices(int count) {
        return java.util.stream.IntStream.range(0, count)
                .mapToObj(i -> new DailyStockPrice(
                        MARKET, CODE, DATE.minusDays(count - 1 - i),
                        1_000L, 60_000L, 61_000L, 59_000L, 62_000L))
                .toList();
    }
}
