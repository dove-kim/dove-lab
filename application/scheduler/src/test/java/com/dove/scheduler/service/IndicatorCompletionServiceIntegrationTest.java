package com.dove.scheduler.service;

import com.dove.indicator.application.service.TechnicalIndicatorQueryService;
import com.dove.indicator.domain.enums.IndicatorType;
import com.dove.indicator.domain.repository.TechnicalIndicatorRepository;
import com.dove.market.domain.enums.MarketType;
import com.dove.scheduler.TestSchedulerApplication;
import com.dove.stock.application.service.DailyStockPriceCommandService;
import com.dove.stock.domain.entity.DailyStockPrice;
import com.dove.stock.domain.repository.DailyStockPriceRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

/** IndicatorCompletionService 통합 테스트 — H2 DB 사용. */
@SpringBootTest(classes = TestSchedulerApplication.class)
class IndicatorCompletionServiceIntegrationTest {

    @Autowired IndicatorCompletionService indicatorCompletionService;
    @Autowired DailyStockPriceCommandService priceCommandService;
    @Autowired TechnicalIndicatorQueryService indicatorQueryService;
    @Autowired TechnicalIndicatorRepository technicalIndicatorRepository;
    @Autowired DailyStockPriceRepository dailyStockPriceRepository;

    static final MarketType MARKET = MarketType.KOSPI;
    static final String CODE = "005930";
    /** 등록된 계산기 중 최대 요구 봉 수 (NewHighLowFlagCalculator.PERIOD_52W = 252) */
    static final int MIN_REQUIRED = 252;

    @AfterEach
    void tearDown() {
        technicalIndicatorRepository.deleteAll();
        dailyStockPriceRepository.deleteAll();
    }

    @Test
    @DisplayName("주가 없음 → 지표 저장 안 됨")
    void shouldDoNothingWhenNoPriceData() {
        LocalDate from = LocalDate.of(2026, 1, 2);
        LocalDate to   = LocalDate.of(2026, 1, 31);

        indicatorCompletionService.scan(MARKET, from, to);

        Map<LocalDate, Map<IndicatorType, Double>> result =
                indicatorQueryService.findRecentByStock(MARKET, CODE, List.of(IndicatorType.SMA_5), 10);
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("주가 있고 지표 누락 → SMA_5 지표 채워짐")
    void shouldFillMissingIndicatorsWhenPriceDataExists() {
        // MACD 계산에 130봉 이상 필요 → 충분한 주가 제공
        LocalDate lastDate = LocalDate.of(2026, 1, 10);
        LocalDate from = lastDate.minusDays(MIN_REQUIRED - 1);
        List<DailyStockPrice> prices = buildPrices(CODE, lastDate, MIN_REQUIRED);
        priceCommandService.saveAll(prices);

        indicatorCompletionService.scan(MARKET, from, lastDate);

        Map<LocalDate, Map<IndicatorType, Double>> result =
                indicatorQueryService.findRecentByStock(MARKET, CODE, List.of(IndicatorType.SMA_5), 5);
        assertThat(result).containsKey(lastDate);
        assertThat(result.get(lastDate).get(IndicatorType.SMA_5)).isPositive();
    }

    @Test
    @DisplayName("일부 날짜만 지표 누락 → 누락 날짜만 재계산됨")
    void shouldOnlyFillMissingDates() {
        LocalDate lastDate = LocalDate.of(2026, 1, 14);
        LocalDate from = lastDate.minusDays(MIN_REQUIRED - 1);
        List<DailyStockPrice> prices = buildPrices(CODE, lastDate, MIN_REQUIRED);
        priceCommandService.saveAll(prices);

        // 전체 기간 1차 계산
        indicatorCompletionService.scan(MARKET, from, lastDate);

        // 마지막 2일의 지표를 삭제하여 누락 상태 만들기
        LocalDate missingDate1 = lastDate.minusDays(1);
        LocalDate missingDate2 = lastDate;
        technicalIndicatorRepository.deleteAll(
                technicalIndicatorRepository.findAll().stream()
                        .filter(ti -> !ti.getId().getTradeDate().isBefore(missingDate1))
                        .toList());

        // 재스캔
        indicatorCompletionService.scan(MARKET, from, lastDate);

        Map<LocalDate, Map<IndicatorType, Double>> result =
                indicatorQueryService.findRecentByStock(MARKET, CODE, List.of(IndicatorType.SMA_5), 10);
        assertThat(result).containsKey(missingDate1);
        assertThat(result).containsKey(missingDate2);
    }

    @Test
    @DisplayName("2개 종목 누락 → 모두 재계산됨")
    void shouldFillMissingForMultipleStocks() {
        String code2 = "000660";
        LocalDate lastDate = LocalDate.of(2026, 1, 10);
        LocalDate from = lastDate.minusDays(MIN_REQUIRED - 1);

        priceCommandService.saveAll(buildPrices(CODE, lastDate, MIN_REQUIRED));
        priceCommandService.saveAll(buildPrices(code2, lastDate, MIN_REQUIRED));

        indicatorCompletionService.scan(MARKET, from, lastDate);

        Map<LocalDate, Map<IndicatorType, Double>> result1 =
                indicatorQueryService.findRecentByStock(MARKET, CODE, List.of(IndicatorType.SMA_5), 3);
        Map<LocalDate, Map<IndicatorType, Double>> result2 =
                indicatorQueryService.findRecentByStock(MARKET, code2, List.of(IndicatorType.SMA_5), 3);

        assertThat(result1).containsKey(lastDate);
        assertThat(result2).containsKey(lastDate);
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    /** targetDate 기준 과거 count일치 주가를 생성한다. */
    private List<DailyStockPrice> buildPrices(String code, LocalDate targetDate, int count) {
        return IntStream.range(0, count)
                .mapToObj(i -> {
                    LocalDate date = targetDate.minusDays(count - 1 - i);
                    long close = 60_000L + (long) i * 100;
                    return new DailyStockPrice(MARKET, code, date,
                            1_000L, close - 200, close, close - 300, close + 300);
                })
                .toList();
    }
}
