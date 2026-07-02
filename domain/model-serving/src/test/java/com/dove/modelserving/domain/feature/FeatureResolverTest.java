package com.dove.modelserving.domain.feature;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("FeatureResolver")
class FeatureResolverTest {

    private final FeatureResolver resolver = new FeatureResolver();

    @Nested
    @DisplayName("resolve")
    class Resolve {

        @Test
        @DisplayName("지표 피처는 STOCK_FEATURE_DAILY의 동명 컬럼으로 해석한다")
        void shouldResolveIndicatorToFeatureTable() {
            assertThat(resolver.resolve("rsi_14"))
                    .contains(new FeatureSource(FeatureTable.STOCK_FEATURE_DAILY, "RSI_14"));
        }

        @Test
        @DisplayName("순위 피처는 STOCK_RANK_DAILY의 동명 컬럼으로 해석한다")
        void shouldResolveRankToRankTable() {
            assertThat(resolver.resolve("rank_turnover"))
                    .contains(new FeatureSource(FeatureTable.STOCK_RANK_DAILY, "RANK_TURNOVER"));
        }

        @Test
        @DisplayName("대문자 입력도 동일하게 해석한다")
        void shouldResolveUppercaseInput() {
            assertThat(resolver.resolve("MACD_HISTOGRAM"))
                    .contains(new FeatureSource(FeatureTable.STOCK_FEATURE_DAILY, "MACD_HISTOGRAM"));
        }

        @Test
        @DisplayName("알 수 없는 이름은 빈 Optional을 반환한다")
        void shouldReturnEmptyWhenUnknown() {
            assertThat(resolver.resolve("made_up_feature")).isEmpty();
        }

        @Test
        @DisplayName("null은 빈 Optional을 반환한다")
        void shouldReturnEmptyWhenNull() {
            assertThat(resolver.resolve(null)).isEmpty();
        }
    }
}
