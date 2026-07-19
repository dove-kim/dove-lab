package com.dove.modelserving.domain.feature;

import com.dove.indicator.domain.entity.StockFeatureDaily;
import com.dove.indicator.domain.entity.StockFeatureDailyId;
import com.dove.indicator.domain.enums.IndicatorType;
import com.dove.stock.domain.enums.PriceType;
import com.dove.stock.domain.enums.StockExchange;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("FeatureRowMapper")
class FeatureRowMapperTest {

    private final FeatureRowMapper mapper = new FeatureRowMapper();

    @Nested
    @DisplayName("toFeatureMap")
    class ToFeatureMap {

        @Test
        @DisplayName("원시 거래량·거래대금을 VOLUME·TURNOVER 키로 지표값과 함께 담는다")
        void shouldIncludeRawVolumeAndTurnover() {
            StockFeatureDaily feature = featureWith(150000L, 2_000_000_000L);
            feature.set(IndicatorType.RSI_14, 55.0);

            Map<String, Double> map = mapper.toFeatureMap(feature, null);

            assertThat(map).containsEntry("VOLUME", 150000.0)
                    .containsEntry("TURNOVER", 2_000_000_000.0)
                    .containsEntry("RSI_14", 55.0);
        }

        @Test
        @DisplayName("원시 시세가 NULL이면 해당 키를 담지 않는다")
        void shouldOmitNullRawColumns() {
            Map<String, Double> map = mapper.toFeatureMap(featureWith(null, null), null);

            assertThat(map).doesNotContainKeys("VOLUME", "TURNOVER");
        }
    }

    private static StockFeatureDaily featureWith(Long volume, Long turnover) {
        StockFeatureDailyId id = new StockFeatureDailyId(
                "005930", StockExchange.INTEGRATED, PriceType.ADJUSTED, LocalDate.of(2026, 7, 16));
        return new StockFeatureDaily(id, 1, null, null, null, null, volume, turnover, LocalDateTime.now());
    }
}
