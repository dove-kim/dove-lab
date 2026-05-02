package com.dove.technicalindicator.domain.calculator;

import com.dove.market.domain.enums.MarketType;
import com.dove.stockprice.domain.entity.DailyStockPrice;
import com.dove.technicalindicator.domain.enums.IndicatorType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

class NewHighLowFlagCalculatorTest {

    private final NewHighLowFlagCalculator calculator = new NewHighLowFlagCalculator();

    private DailyStockPrice createPrice(int dayOffset, long closePrice) {
        return new DailyStockPrice(MarketType.KOSPI, "005930",
                LocalDate.of(2024, 1, 1).plusDays(dayOffset),
                1000L, closePrice, closePrice, closePrice, closePrice);
    }

    private List<DailyStockPrice> build252PricesWithLastClose(long lastClose) {
        List<DailyStockPrice> list = new ArrayList<>();
        for (int i = 0; i < 251; i++) {
            list.add(createPrice(i, 1000L));
        }
        list.add(createPrice(251, lastClose));
        return list;
    }

    @Test
    @DisplayName("마지막 종가가 252일 중 최고가일 때 IS_52W_HIGH는 1.0이다")
    void shouldReturn1WhenCloseIs52WeekHigh() {
        List<DailyStockPrice> data = build252PricesWithLastClose(9999L);

        Map<IndicatorType, Double> result = calculator.calculate(data);

        assertThat(result.get(IndicatorType.IS_52W_HIGH)).isEqualTo(1.0);
        assertThat(result.get(IndicatorType.IS_52W_LOW)).isEqualTo(0.0);
    }

    @Test
    @DisplayName("마지막 종가가 252일 중 최저가일 때 IS_52W_LOW는 1.0이다")
    void shouldReturn1WhenCloseIs52WeekLow() {
        List<DailyStockPrice> data = build252PricesWithLastClose(1L);

        Map<IndicatorType, Double> result = calculator.calculate(data);

        assertThat(result.get(IndicatorType.IS_52W_LOW)).isEqualTo(1.0);
        assertThat(result.get(IndicatorType.IS_52W_HIGH)).isEqualTo(0.0);
    }

    @Test
    @DisplayName("마지막 종가가 마지막 20일 중 최고가일 때 IS_20D_HIGH는 1.0이다")
    void shouldReturn1WhenCloseIs20DayHigh() {
        List<DailyStockPrice> list = new ArrayList<>();
        for (int i = 0; i < 232; i++) {
            list.add(createPrice(i, 1000L));
        }
        for (int i = 232; i < 251; i++) {
            list.add(createPrice(i, 500L));
        }
        list.add(createPrice(251, 9999L));

        Map<IndicatorType, Double> result = calculator.calculate(list);

        assertThat(result.get(IndicatorType.IS_20D_HIGH)).isEqualTo(1.0);
        assertThat(result.get(IndicatorType.IS_20D_LOW)).isEqualTo(0.0);
    }

    @Test
    @DisplayName("마지막 종가가 극단값이 아닐 때 플래그가 모두 0.0이다")
    void shouldReturn0WhenCloseIsNotAtExtremes() {
        // 52주 최고 1200, 최저 400이 존재하고, 마지막 20개 안에도 600~1000이 섞임
        // → 마지막 종가 800은 어떤 극단값도 아님
        List<DailyStockPrice> list = new ArrayList<>();
        list.add(createPrice(0, 400L));   // 52주 최저
        list.add(createPrice(1, 1200L));  // 52주 최고
        for (int i = 2; i < 232; i++) {
            list.add(createPrice(i, 1000L));
        }
        // 마지막 20개: 600, 1000 혼합 → 800은 최저도 최고도 아님
        for (int i = 232; i < 242; i++) {
            list.add(createPrice(i, 600L));
        }
        for (int i = 242; i < 251; i++) {
            list.add(createPrice(i, 1000L));
        }
        list.add(createPrice(251, 800L));

        Map<IndicatorType, Double> result = calculator.calculate(list);

        assertThat(result.get(IndicatorType.IS_52W_HIGH)).isEqualTo(0.0);
        assertThat(result.get(IndicatorType.IS_52W_LOW)).isEqualTo(0.0);
        assertThat(result.get(IndicatorType.IS_20D_HIGH)).isEqualTo(0.0);
        assertThat(result.get(IndicatorType.IS_20D_LOW)).isEqualTo(0.0);
    }

    @Test
    @DisplayName("데이터가 252개 미만이면 빈 Map을 반환한다")
    void shouldReturnEmptyWhenDataInsufficient() {
        List<DailyStockPrice> data = IntStream.range(0, 251)
                .mapToObj(i -> createPrice(i, 1000L))
                .toList();

        Map<IndicatorType, Double> result = calculator.calculate(data);

        assertThat(result).isEmpty();
    }
}
