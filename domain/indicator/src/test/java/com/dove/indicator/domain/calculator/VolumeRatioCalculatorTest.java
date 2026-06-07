package com.dove.indicator.domain.calculator;

import com.dove.stock.domain.entity.StockPrice;
import com.dove.stock.domain.enums.StockExchange;
import com.dove.stock.domain.enums.PriceType;
import com.dove.indicator.domain.calculator.VolumeRatioCalculator;
import com.dove.indicator.domain.enums.IndicatorType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class VolumeRatioCalculatorTest {

    private final VolumeRatioCalculator calculator = new VolumeRatioCalculator();

    private StockPrice createStockPrice(LocalDate date, long closePrice, long volume) {
        return new StockPrice("005930", StockExchange.KOSPI, PriceType.RAW, date,
                100L, 110L, 90L, closePrice, volume, null);
    }

    @Test
    @DisplayName("알려진 값으로 Volume Ratio를 검증한다")
    void shouldCalculateVolumeRatioFromKnownValues() {
        // Given - 21개 데이터
        // 10일 상승(vol=1000), 10일 하락(vol=500) → VR = 1000*10 / 500*10 * 100 = 200
        List<StockPrice> data = new ArrayList<>();
        data.add(createStockPrice(LocalDate.of(2024, 1, 1), 1000, 1000));
        for (int i = 1; i <= 10; i++) {
            data.add(createStockPrice(LocalDate.of(2024, 1, 1).plusDays(i), 1000 + i, 1000));
        }
        for (int i = 11; i <= 20; i++) {
            data.add(createStockPrice(LocalDate.of(2024, 1, 1).plusDays(i), 1010 - (i - 10), 500));
        }

        // When
        Map<IndicatorType, Double> result = calculator.calculate(data);

        // Then
        assertThat(result.get(IndicatorType.VOLUME_RATIO_20)).isCloseTo(200.0, within(0.01));
    }

    @Test
    @DisplayName("상승 거래량과 하락 거래량이 같으면 100을 반환한다")
    void shouldReturn100WhenUpVolumeEqualsDownVolume() {
        // Given - 교대로 상승/하락, 동일 거래량
        List<StockPrice> data = new ArrayList<>();
        data.add(createStockPrice(LocalDate.of(2024, 1, 1), 1000, 1000));
        for (int i = 1; i <= 20; i++) {
            long price = (i % 2 == 1) ? 1001 : 999;
            data.add(createStockPrice(LocalDate.of(2024, 1, 1).plusDays(i), price, 1000));
        }

        // When
        Map<IndicatorType, Double> result = calculator.calculate(data);

        // Then
        assertThat(result.get(IndicatorType.VOLUME_RATIO_20)).isCloseTo(100.0, within(0.01));
    }

    @Test
    @DisplayName("indicatorType()은 VOLUME_RATIO_20를 반환한다")
    void shouldReturnVolumeRatioAsCursorType() {
        assertThat(calculator.indicatorType()).isEqualTo(IndicatorType.VOLUME_RATIO_20);
    }
}
