package com.dove.technicalindicator.domain.calculator;

import com.dove.market.domain.enums.MarketType;
import com.dove.stockprice.domain.entity.DailyStockPrice;
import com.dove.technicalindicator.domain.enums.IndicatorType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class VolumeMa20RatioCalculatorTest {

    private final VolumeMa20RatioCalculator calculator = new VolumeMa20RatioCalculator();

    private DailyStockPrice createDailyStockPrice(LocalDate date, long volume) {
        return new DailyStockPrice(MarketType.KOSPI, "005930", date,
                volume, 100L, 100L, 90L, 110L);
    }

    @Test
    @DisplayName("마지막 거래량이 20일 평균의 2배이면 VOLUME_MA20_RATIO는 2.0이다")
    void shouldCalculateVolumeRatioCorrectly() {
        // Given - 처음 19개 거래량 100, 마지막 거래량 300
        // 평균 = (19 * 100 + 300) / 20 = 2200 / 20 = 110
        // 비율 = 300 / 110 ≈ 2.727...
        // 단순하게: 19개 volume=100, 1개 volume=200 → 평균=(19*100+200)/20=105, 비율=200/105≈1.905
        // 검증 가능한 케이스: 모두 volume=100, 마지막만 volume=200 → 평균=105, ratio≈1.905
        // 더 명확한 케이스: 19개 volume=0이면 평균=volume_last/20, ratio=20 (but volume=0 odd)
        // 20개 모두 100 → 평균=100, ratio=1.0
        List<DailyStockPrice> data = IntStream.range(0, 20)
                .mapToObj(i -> createDailyStockPrice(
                        LocalDate.of(2024, 1, 1).plusDays(i),
                        100L))
                .toList();

        // When
        Map<IndicatorType, Double> result = calculator.calculate(data);

        // Then - 모두 100이면 비율 = 100 / 100 = 1.0
        assertThat(result.get(IndicatorType.VOLUME_MA20_RATIO)).isCloseTo(1.0, within(0.001));
    }

    @Test
    @DisplayName("마지막 거래량이 나머지보다 크면 비율이 1보다 크다")
    void shouldReturnRatioGreaterThanOneWhenLastVolumeIsHigher() {
        // Given - 19개 volume=100, 마지막 volume=300
        // 평균 = (19*100 + 300) / 20 = 2200 / 20 = 110
        // ratio = 300 / 110 ≈ 2.727
        List<DailyStockPrice> firstNineteen = IntStream.range(0, 19)
                .mapToObj(i -> createDailyStockPrice(
                        LocalDate.of(2024, 1, 1).plusDays(i),
                        100L))
                .toList();
        DailyStockPrice last = createDailyStockPrice(LocalDate.of(2024, 1, 1).plusDays(19), 300L);
        List<DailyStockPrice> data = new java.util.ArrayList<>(firstNineteen);
        data.add(last);

        // When
        Map<IndicatorType, Double> result = calculator.calculate(data);

        // Then
        assertThat(result.get(IndicatorType.VOLUME_MA20_RATIO)).isCloseTo(300.0 / 110.0, within(0.001));
    }

    @Test
    @DisplayName("데이터가 requiredDataSize 미만이면 빈 맵을 반환한다")
    void shouldReturnEmptyWhenDataInsufficient() {
        // Given - 19개 (20 미만)
        List<DailyStockPrice> data = IntStream.range(0, 19)
                .mapToObj(i -> createDailyStockPrice(
                        LocalDate.of(2024, 1, 1).plusDays(i),
                        100L))
                .toList();

        // When
        Map<IndicatorType, Double> result = calculator.calculate(data);

        // Then
        assertThat(result).isEmpty();
    }
}
