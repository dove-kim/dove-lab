package com.dove.scheduler.service;

import com.dove.indicator.application.service.IndicatorBulkCalculateService;
import com.dove.indicator.application.service.TechnicalIndicatorQueryService;
import com.dove.market.domain.enums.MarketType;
import com.dove.stock.application.service.DailyStockPriceQueryService;
import com.dove.stock.domain.entity.DailyStockPrice;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IndicatorCompletionServiceTest {

    @Mock DailyStockPriceQueryService dailyStockPriceQueryService;
    @Mock TechnicalIndicatorQueryService technicalIndicatorQueryService;
    @Mock IndicatorBulkCalculateService indicatorBulkCalculateService;

    IndicatorCompletionService service;

    static final MarketType MARKET = MarketType.KOSPI;
    static final LocalDate FROM   = LocalDate.of(2026, 1, 1);
    static final LocalDate UP_TO  = LocalDate.of(2026, 1, 31);
    static final String CODE      = "005930";

    @BeforeEach
    void setUp() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.initialize();
        service = new IndicatorCompletionService(
                dailyStockPriceQueryService, technicalIndicatorQueryService,
                indicatorBulkCalculateService, executor);
    }

    @Test
    @DisplayName("주가 데이터 없음 → calculateAndSave 호출 없음")
    void shouldDoNothingWhenNoPriceData() {
        when(dailyStockPriceQueryService.findPriceDatesByStock(MARKET, FROM, UP_TO))
                .thenReturn(Map.of());

        service.scan(MARKET, FROM, UP_TO);

        verify(indicatorBulkCalculateService, never()).calculateAndSave(any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("모든 날짜 완성 → calculateAndSave 호출 없음")
    void shouldDoNothingWhenAllCalculated() {
        LocalDate d = LocalDate.of(2026, 1, 2);
        when(dailyStockPriceQueryService.findPriceDatesByStock(MARKET, FROM, UP_TO))
                .thenReturn(Map.of(CODE, new TreeSet<>(Set.of(d))));
        when(technicalIndicatorQueryService.findCompletedDatesPerStock(
                eq(MARKET), eq(FROM), eq(UP_TO), anyLong()))
                .thenReturn(Map.of(CODE, Set.of(d)));

        service.scan(MARKET, FROM, UP_TO);

        verify(indicatorBulkCalculateService, never()).calculateAndSave(any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("일부 날짜 누락 → 누락 날짜만 포함하여 calculateAndSave 호출")
    void shouldCalculateMissingDatesForStock() {
        LocalDate calculated = LocalDate.of(2026, 1, 2);
        LocalDate missing    = LocalDate.of(2026, 1, 5);
        when(dailyStockPriceQueryService.findPriceDatesByStock(MARKET, FROM, UP_TO))
                .thenReturn(Map.of(CODE, new TreeSet<>(Set.of(calculated, missing))));
        when(technicalIndicatorQueryService.findCompletedDatesPerStock(
                eq(MARKET), eq(FROM), eq(UP_TO), anyLong()))
                .thenReturn(Map.of(CODE, Set.of(calculated)));
        when(dailyStockPriceQueryService.findAllByMarketAndCode(MARKET, CODE))
                .thenReturn(List.of());
        when(indicatorBulkCalculateService.maxRequiredDataSize()).thenReturn(0); // 단위 테스트: 데이터 부족 스킵 비활성화

        service.scan(MARKET, FROM, UP_TO);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Set<LocalDate>> datesCaptor = ArgumentCaptor.forClass(Set.class);
        verify(indicatorBulkCalculateService).calculateAndSave(
                eq(MARKET), eq(CODE), any(), datesCaptor.capture(), any());
        assertThat(datesCaptor.getValue()).containsExactly(missing);
    }

    @Test
    @DisplayName("지표 없는 신규 종목 → 전체 날짜로 calculateAndSave 호출")
    void shouldCalculateForNewStock() {
        LocalDate d1 = LocalDate.of(2026, 1, 2);
        LocalDate d2 = LocalDate.of(2026, 1, 5);
        when(dailyStockPriceQueryService.findPriceDatesByStock(MARKET, FROM, UP_TO))
                .thenReturn(Map.of(CODE, new TreeSet<>(Set.of(d1, d2))));
        when(technicalIndicatorQueryService.findCompletedDatesPerStock(
                eq(MARKET), eq(FROM), eq(UP_TO), anyLong()))
                .thenReturn(Map.of()); // 지표 없음
        when(dailyStockPriceQueryService.findAllByMarketAndCode(MARKET, CODE))
                .thenReturn(List.of());
        when(indicatorBulkCalculateService.maxRequiredDataSize()).thenReturn(0);

        service.scan(MARKET, FROM, UP_TO);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Set<LocalDate>> datesCaptor = ArgumentCaptor.forClass(Set.class);
        verify(indicatorBulkCalculateService).calculateAndSave(
                eq(MARKET), eq(CODE), any(), datesCaptor.capture(), any());
        assertThat(datesCaptor.getValue()).containsExactlyInAnyOrder(d1, d2);
    }

    @Test
    @DisplayName("2개 종목 각각 누락 → calculateAndSave 2번 호출")
    void shouldCalculateForMultipleStocks() {
        String code2 = "000660";
        LocalDate d1 = LocalDate.of(2026, 1, 2);
        LocalDate d2 = LocalDate.of(2026, 1, 5);
        LocalDate d3 = LocalDate.of(2026, 1, 9);
        when(dailyStockPriceQueryService.findPriceDatesByStock(MARKET, FROM, UP_TO))
                .thenReturn(Map.of(
                        CODE,  new TreeSet<>(Set.of(d1, d2)),
                        code2, new TreeSet<>(Set.of(d2, d3))));
        when(technicalIndicatorQueryService.findCompletedDatesPerStock(
                eq(MARKET), eq(FROM), eq(UP_TO), anyLong()))
                .thenReturn(Map.of(
                        CODE,  Set.of(d1),  // d2 누락
                        code2, Set.of(d3))); // d2 누락
        when(dailyStockPriceQueryService.findAllByMarketAndCode(eq(MARKET), any()))
                .thenReturn(List.of());
        when(indicatorBulkCalculateService.maxRequiredDataSize()).thenReturn(0);

        service.scan(MARKET, FROM, UP_TO);

        verify(indicatorBulkCalculateService, times(2))
                .calculateAndSave(eq(MARKET), any(), any(), any(), any());
    }

    @Test
    @DisplayName("주가 이력이 최소 봉 수 미만 → calculateAndSave 건너뜀 (신규 상장 보호)")
    void shouldSkipStockWithInsufficientPriceHistory() {
        LocalDate d = LocalDate.of(2026, 1, 2);
        int maxRequired = 130;
        List<DailyStockPrice> tooFewPrices = mockPrices(maxRequired - 1);

        when(dailyStockPriceQueryService.findPriceDatesByStock(MARKET, FROM, UP_TO))
                .thenReturn(Map.of(CODE, new TreeSet<>(Set.of(d))));
        when(technicalIndicatorQueryService.findCompletedDatesPerStock(
                eq(MARKET), eq(FROM), eq(UP_TO), anyLong()))
                .thenReturn(Map.of()); // 지표 없음
        when(dailyStockPriceQueryService.findAllByMarketAndCode(MARKET, CODE))
                .thenReturn(tooFewPrices);
        when(indicatorBulkCalculateService.maxRequiredDataSize()).thenReturn(maxRequired);

        service.scan(MARKET, FROM, UP_TO);

        verify(indicatorBulkCalculateService, never()).calculateAndSave(any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("주가 이력이 최소 봉 수 이상 → calculateAndSave 정상 호출")
    void shouldCalculateWhenPriceHistorySufficient() {
        LocalDate d = LocalDate.of(2026, 1, 2);
        int maxRequired = 130;
        List<DailyStockPrice> enoughPrices = mockPrices(maxRequired);

        when(dailyStockPriceQueryService.findPriceDatesByStock(MARKET, FROM, UP_TO))
                .thenReturn(Map.of(CODE, new TreeSet<>(Set.of(d))));
        when(technicalIndicatorQueryService.findCompletedDatesPerStock(
                eq(MARKET), eq(FROM), eq(UP_TO), anyLong()))
                .thenReturn(Map.of());
        when(dailyStockPriceQueryService.findAllByMarketAndCode(MARKET, CODE))
                .thenReturn(enoughPrices);
        when(indicatorBulkCalculateService.maxRequiredDataSize()).thenReturn(maxRequired);

        service.scan(MARKET, FROM, UP_TO);

        verify(indicatorBulkCalculateService).calculateAndSave(eq(MARKET), eq(CODE), any(), any(), any());
    }

    // -------------------------------------------------------------------------
    // 헬퍼
    // -------------------------------------------------------------------------

    /** 크기만 맞추는 DailyStockPrice 목 리스트 */
    private static List<DailyStockPrice> mockPrices(int count) {
        List<DailyStockPrice> list = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            list.add(mock(DailyStockPrice.class));
        }
        return list;
    }
}
