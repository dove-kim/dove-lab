package com.dove.technicalindicator.domain.calculator;

import com.dove.stockdata.domain.entity.StockData;
import com.dove.stockdata.domain.enums.MarketType;
import com.dove.technicalindicator.domain.calculator.VolumeRatioCalculator;
import com.dove.technicalindicator.domain.enums.IndicatorType;
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

    private StockData createStockData(LocalDate date, long closePrice, long volume) {
        return new StockData(MarketType.KOSPI, "005930", date,
                volume, 100L, closePrice, 90L, 110L);
    }

    @Test
    @DisplayName("알려진 값으로 Volume Ratio를 검증한다")
    void shouldCalculateVolumeRatioFromKnownValues() {
        // Given - 21개 데이터
        // 10일 상승(vol=1000), 10일 하락(vol=500) → VR = 1000*10 / 500*10 * 100 = 200
        List<StockData> data = new ArrayList<>();
        data.add(createStockData(LocalDate.of(2024, 1, 1), 1000, 1000));
        for (int i = 1; i <= 10; i++) {
            data.add(createStockData(LocalDate.of(2024, 1, 1).plusDays(i), 1000 + i, 1000));
        }
        for (int i = 11; i <= 20; i++) {
            data.add(createStockData(LocalDate.of(2024, 1, 1).plusDays(i), 1010 - (i - 10), 500));
        }

        // When
        Map<IndicatorType, Double> result = calculator.calculate(data);

        // Then
        assertThat(result.get(IndicatorType.VOLUME_RATIO)).isCloseTo(200.0, within(0.01));
    }

    @Test
    @DisplayName("상승 거래량과 하락 거래량이 같으면 100을 반환한다")
    void shouldReturn100WhenUpVolumeEqualsDownVolume() {
        // Given - 교대로 상승/하락, 동일 거래량
        List<StockData> data = new ArrayList<>();
        data.add(createStockData(LocalDate.of(2024, 1, 1), 1000, 1000));
        for (int i = 1; i <= 20; i++) {
            long price = (i % 2 == 1) ? 1001 : 999;
            data.add(createStockData(LocalDate.of(2024, 1, 1).plusDays(i), price, 1000));
        }

        // When
        Map<IndicatorType, Double> result = calculator.calculate(data);

        // Then
        assertThat(result.get(IndicatorType.VOLUME_RATIO)).isCloseTo(100.0, within(0.01));
    }
}
