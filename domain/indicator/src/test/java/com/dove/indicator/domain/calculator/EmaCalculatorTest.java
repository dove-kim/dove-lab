package com.dove.indicator.domain.calculator;

import com.dove.market.domain.enums.MarketType;
import com.dove.stock.domain.entity.DailyStockPrice;
import com.dove.indicator.domain.enums.IndicatorType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class EmaCalculatorTest {

    private final EmaCalculator ema5 = new EmaCalculator(5, IndicatorType.EMA_5);

    private DailyStockPrice price(int day, long close) {
        return new DailyStockPrice(MarketType.KOSPI, "005930",
                LocalDate.of(2024, 1, day), 1000L, 100L, close, 90L, 110L);
    }

    // ─── 메타 ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("getName() — IndicatorType 이름 반환")
    void shouldReturnName() {
        assertThat(ema5.getName()).isEqualTo("EMA_5");
    }

    @Test
    @DisplayName("requiredDataSize() — period 반환")
    void shouldReturnPeriodAsRequiredDataSize() {
        assertThat(ema5.requiredDataSize()).isEqualTo(5);
        assertThat(new EmaCalculator(20, IndicatorType.EMA_20).requiredDataSize()).isEqualTo(20);
    }

    @Test
    @DisplayName("indicatorType() — IndicatorType 반환")
    void shouldReturnCursorType() {
        assertThat(ema5.indicatorType()).isEqualTo(IndicatorType.EMA_5);
    }

    // ─── calculate() ─────────────────────────────────────────────────────

    @Test
    @DisplayName("calculate() — period 개 데이터만 있으면 SMA와 동일")
    void shouldReturnSmaWhenExactlyPeriodDataPoints() {
        // pool 크기 == period → 루프 0회 → 단순 평균
        // [100, 200, 300, 400, 500] → SMA5 = 300
        List<DailyStockPrice> pool = IntStream.rangeClosed(1, 5)
                .mapToObj(i -> price(i, i * 100L))
                .toList();

        Map<IndicatorType, Double> result = ema5.calculate(pool);

        assertThat(result.get(IndicatorType.EMA_5)).isCloseTo(300.0, within(0.01));
    }

    @Test
    @DisplayName("calculate() — period 초과 데이터에 EMA 가중 적용")
    void shouldApplyEmaWeightingBeyondPeriod() {
        // [100, 200, 300, 400, 500, 600]
        // multiplier = 2/6 = 1/3
        // 초기 SMA(1~5) = 300
        // EMA after 600: 600*(1/3) + 300*(2/3) = 200 + 200 = 400
        List<DailyStockPrice> pool = IntStream.rangeClosed(1, 6)
                .mapToObj(i -> price(i, i * 100L))
                .toList();

        Map<IndicatorType, Double> result = ema5.calculate(pool);

        assertThat(result.get(IndicatorType.EMA_5)).isCloseTo(400.0, within(0.01));
    }

    @Test
    @DisplayName("calculate() — 모든 값이 같으면 EMA도 그 값")
    void shouldReturnSameValueWhenAllPricesEqual() {
        List<DailyStockPrice> pool = IntStream.rangeClosed(1, 10)
                .mapToObj(i -> price(i, 1000L))
                .toList();

        Map<IndicatorType, Double> result = ema5.calculate(pool);

        assertThat(result.get(IndicatorType.EMA_5)).isCloseTo(1000.0, within(0.01));
    }

    @Test
    @DisplayName("calculate() — 종가만 사용 (거래량·시가 등 무시)")
    void shouldUseOnlyClosePrice() {
        // 모든 OHLCV가 9999이지만 closePrice만 100
        List<DailyStockPrice> pool = IntStream.rangeClosed(1, 5)
                .mapToObj(i -> new DailyStockPrice(MarketType.KOSPI, "005930",
                        LocalDate.of(2024, 1, i), 9999L, 9999L, 100L, 1L, 9999L))
                .toList();

        Map<IndicatorType, Double> result = ema5.calculate(pool);

        assertThat(result.get(IndicatorType.EMA_5)).isCloseTo(100.0, within(0.01));
    }

    // ─── calculateWithSeed() ──────────────────────────────────────────────

    @Test
    @DisplayName("calculateWithSeed() — seed로 이전 EMA 적용해 단일 가격 계산")
    void shouldCalculateWithSeed() {
        // seed(이전 EMA) = 300, 최신 종가 = 600
        // multiplier = 2/(5+1) = 1/3
        // EMA = 600*(1/3) + 300*(2/3) = 200 + 200 = 400
        List<DailyStockPrice> pool = List.of(price(1, 600L));

        Map<IndicatorType, Double> result = ema5.calculateWithSeed(pool, 300.0);

        assertThat(result.get(IndicatorType.EMA_5)).isCloseTo(400.0, within(0.01));
    }

    @Test
    @DisplayName("calculateWithSeed() — seed == 종가면 EMA 변화 없음")
    void shouldReturnSameEmaWhenSeedEqualClosePrice() {
        // seed = 1000, 종가 = 1000 → EMA = 1000
        List<DailyStockPrice> pool = List.of(price(1, 1000L));

        Map<IndicatorType, Double> result = ema5.calculateWithSeed(pool, 1000.0);

        assertThat(result.get(IndicatorType.EMA_5)).isCloseTo(1000.0, within(0.01));
    }

    @Test
    @DisplayName("calculateWithSeed() — pool 마지막 원소의 종가를 사용")
    void shouldUseLastElementClosePrice() {
        // pool에 여러 원소가 있어도 마지막 것만 사용
        // seed = 300, last close = 600
        // EMA = 600*(1/3) + 300*(2/3) = 400
        List<DailyStockPrice> pool = List.of(
                price(1, 100L),
                price(2, 200L),
                price(3, 600L));

        Map<IndicatorType, Double> result = ema5.calculateWithSeed(pool, 300.0);

        assertThat(result.get(IndicatorType.EMA_5)).isCloseTo(400.0, within(0.01));
    }
}
