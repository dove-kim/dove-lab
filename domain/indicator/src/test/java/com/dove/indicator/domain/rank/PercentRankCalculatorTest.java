package com.dove.indicator.domain.rank;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * universe 내 횡단면 PERCENT_RANK(0~1) 계산 검증.
 */
class PercentRankCalculatorTest {

    private final PercentRankCalculator calculator = new PercentRankCalculator();

    @Nested
    @DisplayName("percentRank")
    class PercentRank {

        @Test
        @DisplayName("오름차순 percentile — 최소는 0, 최대는 1, 중간은 (rank-1)/(n-1)")
        void shouldRankAscendingFromZeroToOne() {
            Map<String, Double> values = new LinkedHashMap<>();
            values.put("a", 10.0);
            values.put("b", 20.0);
            values.put("c", 30.0);
            values.put("d", 40.0);
            values.put("e", 50.0);

            Map<String, Double> ranks = calculator.percentRank(values);

            assertThat(ranks.get("a")).isCloseTo(0.0, within(1e-9));
            assertThat(ranks.get("b")).isCloseTo(0.25, within(1e-9));
            assertThat(ranks.get("c")).isCloseTo(0.5, within(1e-9));
            assertThat(ranks.get("d")).isCloseTo(0.75, within(1e-9));
            assertThat(ranks.get("e")).isCloseTo(1.0, within(1e-9));
        }

        @Test
        @DisplayName("동순위(tie)는 같은 percentile을 가지며 그 그룹의 최소 순위를 공유한다")
        void shouldGiveTiedValuesTheSameLowestRank() {
            Map<String, Double> values = new LinkedHashMap<>();
            values.put("a", 10.0);
            values.put("b", 20.0);
            values.put("c", 20.0);
            values.put("d", 40.0);

            Map<String, Double> ranks = calculator.percentRank(values);

            // 정렬: 10(rank1), 20(rank2), 20(rank2), 40(rank4) → (rank-1)/(n-1), n=4
            assertThat(ranks.get("a")).isCloseTo(0.0, within(1e-9));            // (1-1)/3
            assertThat(ranks.get("b")).isCloseTo(1.0 / 3.0, within(1e-9));      // (2-1)/3
            assertThat(ranks.get("c")).isCloseTo(1.0 / 3.0, within(1e-9));      // tie와 동일
            assertThat(ranks.get("d")).isCloseTo(1.0, within(1e-9));           // (4-1)/3
        }

        @Test
        @DisplayName("NULL 값은 순위에서 제외되어 결과에 포함되지 않는다")
        void shouldExcludeNullValues() {
            Map<String, Double> values = new LinkedHashMap<>();
            values.put("a", 10.0);
            values.put("b", null);
            values.put("c", 30.0);

            Map<String, Double> ranks = calculator.percentRank(values);

            assertThat(ranks).containsOnlyKeys("a", "c");
            assertThat(ranks.get("a")).isCloseTo(0.0, within(1e-9));
            assertThat(ranks.get("c")).isCloseTo(1.0, within(1e-9));
        }

        @Test
        @DisplayName("universe에 값이 하나뿐이면 percentile은 0이다 (분모 0 회피)")
        void shouldReturnZeroForSingleValue() {
            Map<String, Double> values = new LinkedHashMap<>();
            values.put("a", 42.0);

            Map<String, Double> ranks = calculator.percentRank(values);

            assertThat(ranks.get("a")).isCloseTo(0.0, within(1e-9));
        }

        @Test
        @DisplayName("모두 같은 값이면 전부 0이다 (분자 0)")
        void shouldReturnZeroWhenAllEqual() {
            Map<String, Double> values = new LinkedHashMap<>();
            values.put("a", 5.0);
            values.put("b", 5.0);
            values.put("c", 5.0);

            Map<String, Double> ranks = calculator.percentRank(values);

            assertThat(ranks.values()).allSatisfy(v -> assertThat(v).isCloseTo(0.0, within(1e-9)));
        }

        @Test
        @DisplayName("값이 없으면 빈 맵을 반환한다")
        void shouldReturnEmptyForNoValues() {
            assertThat(calculator.percentRank(Map.of())).isEmpty();
        }
    }
}
