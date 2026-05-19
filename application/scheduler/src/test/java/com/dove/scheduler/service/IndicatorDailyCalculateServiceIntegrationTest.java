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
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

/** IndicatorDailyCalculateService 통합 테스트 — H2 DB 사용. */
@SpringBootTest(classes = TestSchedulerApplication.class)
class IndicatorDailyCalculateServiceIntegrationTest {

    @Autowired IndicatorDailyCalculateService indicatorDailyCalculateService;
    @Autowired DailyStockPriceCommandService priceCommandService;
    @Autowired TechnicalIndicatorQueryService indicatorQueryService;
    @Autowired TechnicalIndicatorRepository technicalIndicatorRepository;
    @Autowired DailyStockPriceRepository dailyStockPriceRepository;

    static final MarketType MARKET = MarketType.KOSPI;
    static final String CODE = "005930";

    @AfterEach
    void tearDown() {
        technicalIndicatorRepository.deleteAll();
        dailyStockPriceRepository.deleteAll();
    }

    @Test
    @DisplayName("5일치 주가 데이터 → SMA_5 지표 저장됨")
    void shouldSaveSma5WhenFivePriceRecordsExist() {
        // given: 5일치 주가 저장
        LocalDate targetDate = LocalDate.of(2026, 4, 17);
        List<DailyStockPrice> prices = buildPrices(CODE, targetDate, 5);
        priceCommandService.saveAll(prices);

        // when
        indicatorDailyCalculateService.calculateAndSave(MARKET, targetDate);

        // then: SMA_5 지표가 저장됨
        Map<LocalDate, Map<IndicatorType, Double>> result =
                indicatorQueryService.findRecentByStock(MARKET, CODE, List.of(IndicatorType.SMA_5), 1);
        assertThat(result).containsKey(targetDate);
        assertThat(result.get(targetDate)).containsKey(IndicatorType.SMA_5);
        assertThat(result.get(targetDate).get(IndicatorType.SMA_5)).isPositive();
    }

    @Test
    @DisplayName("데이터 부족(4일) → SMA_5 저장 안 됨")
    void shouldNotSaveSma5WhenDataInsufficient() {
        // given: 4일치 주가 저장 (SMA_5 requiredSize=5 미달)
        LocalDate targetDate = LocalDate.of(2026, 4, 17);
        List<DailyStockPrice> prices = buildPrices(CODE, targetDate, 4);
        priceCommandService.saveAll(prices);

        // when
        indicatorDailyCalculateService.calculateAndSave(MARKET, targetDate);

        // then: 저장된 지표 없음
        Map<LocalDate, Map<IndicatorType, Double>> result =
                indicatorQueryService.findRecentByStock(MARKET, CODE, List.of(IndicatorType.SMA_5), 1);
        assertThat(result).doesNotContainKey(targetDate);
    }

    @Test
    @DisplayName("주가 없는 날짜 → 지표 저장 안 됨")
    void shouldNotSaveIndicatorsWhenNoPriceDataForDate() {
        // given: targetDate에는 데이터 없음
        LocalDate targetDate = LocalDate.of(2026, 4, 17);

        // when
        indicatorDailyCalculateService.calculateAndSave(MARKET, targetDate);

        // then
        Map<LocalDate, Map<IndicatorType, Double>> result =
                indicatorQueryService.findRecentByStock(MARKET, CODE, List.of(IndicatorType.SMA_5), 1);
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("calculateRange — 미계산 날짜 지표 저장됨")
    void shouldSaveIndicatorsForMissingDatesInRange() {
        // given: 3일치 주가 저장 (d1, d2, d3)
        LocalDate d1 = LocalDate.of(2026, 4, 14);
        LocalDate d2 = LocalDate.of(2026, 4, 15);
        LocalDate d3 = LocalDate.of(2026, 4, 16);
        // 슬라이딩 윈도우가 작동하려면 d1~d3 각각에 최소 5일치 lookback이 필요
        // → d1부터 과거 5일(d1-4 ~ d1)까지 포함해 저장
        List<DailyStockPrice> allPrices = buildPrices(CODE, d3, 7); // d3 기준 7일치
        priceCommandService.saveAll(allPrices);

        // when
        indicatorDailyCalculateService.calculateRange(MARKET, d1, d3);

        // then: d3 시점의 SMA_5가 저장돼 있어야 함
        Map<LocalDate, Map<IndicatorType, Double>> result =
                indicatorQueryService.findRecentByStock(MARKET, CODE, List.of(IndicatorType.SMA_5), 5);
        assertThat(result).containsKey(d3);
    }

    @Test
    @DisplayName("calculateRange — 이미 계산된 날짜 재계산 안 함")
    void shouldNotRecalculateAlreadyCalculatedDates() {
        // given: d1에 주가 + 지표 모두 존재, d2에는 주가만 존재
        LocalDate d1 = LocalDate.of(2026, 4, 14);
        LocalDate d2 = LocalDate.of(2026, 4, 15);
        List<DailyStockPrice> prices = buildPrices(CODE, d2, 6);
        priceCommandService.saveAll(prices);

        // d1 먼저 계산
        indicatorDailyCalculateService.calculateAndSave(MARKET, d1);

        // d1 지표 값 기록
        Map<LocalDate, Map<IndicatorType, Double>> before =
                indicatorQueryService.findRecentByStock(MARKET, CODE, List.of(IndicatorType.SMA_5), 5);
        double sma5d1Before = before.getOrDefault(d1, Map.of())
                .getOrDefault(IndicatorType.SMA_5, -1.0);

        // when: range 재계산 (d1은 이미 계산됨 → 스킵, d2는 계산)
        indicatorDailyCalculateService.calculateRange(MARKET, d1, d2);

        // then: d1 값은 변경 없음, d2도 계산됨
        Map<LocalDate, Map<IndicatorType, Double>> after =
                indicatorQueryService.findRecentByStock(MARKET, CODE, List.of(IndicatorType.SMA_5), 5);
        double sma5d1After = after.getOrDefault(d1, Map.of())
                .getOrDefault(IndicatorType.SMA_5, -1.0);
        assertThat(sma5d1After).isEqualTo(sma5d1Before);
        assertThat(after).containsKey(d2);
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    /** targetDate 기준 과거 count일치 주가를 생성한다. 종가는 단조증가. */
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
