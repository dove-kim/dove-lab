package com.dove.modelserving.domain.zone;

import com.dove.modelserving.domain.feature.FeatureResolver;
import com.dove.modelserving.domain.meta.ModelEntryZone;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("EntryZoneParser")
class EntryZoneParserTest {

    private final EntryZoneParser parser = new EntryZoneParser(new FeatureResolver());

    @Nested
    @DisplayName("parse")
    class Parse {

        @Test
        @DisplayName("챔피언 진입존 조건을 모두 만족하는 행만 통과시킨다")
        void shouldMatchOnlyChampionEntryRows() {
            ModelEntryZone zone = champion();
            EntryZone parsed = parser.parse(zone);

            Map<String, Double> current = Map.of(
                    "RSI_14", 55.0, "MACD_HISTOGRAM", 0.3, "RANK_TURNOVER", 0.7);
            Map<String, Double> previous = Map.of("RSI_14", 45.0);

            assertThat(parsed.matches(current, previous)).isTrue();
        }

        @Test
        @DisplayName("직전일 RSI가 50 이상이면(상향돌파 아님) 통과시키지 않는다")
        void shouldRejectWhenNotCrossingUp() {
            EntryZone parsed = parser.parse(champion());

            Map<String, Double> current = Map.of(
                    "RSI_14", 55.0, "MACD_HISTOGRAM", 0.3, "RANK_TURNOVER", 0.7);
            Map<String, Double> previous = Map.of("RSI_14", 52.0);

            assertThat(parsed.matches(current, previous)).isFalse();
        }

        @Test
        @DisplayName("rank_turnover 미달이면 통과시키지 않는다")
        void shouldRejectWhenTurnoverBelowThreshold() {
            EntryZone parsed = parser.parse(champion());

            Map<String, Double> current = Map.of(
                    "RSI_14", 55.0, "MACD_HISTOGRAM", 0.3, "RANK_TURNOVER", 0.4);
            Map<String, Double> previous = Map.of("RSI_14", 45.0);

            assertThat(parsed.matches(current, previous)).isFalse();
        }

        @Test
        @DisplayName("prev 값이 없으면 fail-closed로 통과시키지 않는다")
        void shouldRejectWhenPreviousMissing() {
            EntryZone parsed = parser.parse(champion());

            Map<String, Double> current = Map.of(
                    "RSI_14", 55.0, "MACD_HISTOGRAM", 0.3, "RANK_TURNOVER", 0.7);

            assertThat(parsed.matches(current, Map.of())).isFalse();
        }

        @Test
        @DisplayName("파싱 불가 조건이 있으면 fail-closed로 빈 존을 만든다")
        void shouldFailClosedWhenUnparseableCondition() {
            ModelEntryZone zone = new ModelEntryZone("bad", List.of("rsi_14>=50", "garbage"));
            EntryZone parsed = parser.parse(zone);

            assertThat(parsed.conditions()).isEmpty();
            assertThat(parsed.matches(Map.of("RSI_14", 99.0), Map.of())).isFalse();
        }

        @Test
        @DisplayName("미지 피처 조건이 있으면 fail-closed로 빈 존을 만든다")
        void shouldFailClosedWhenUnknownFeature() {
            ModelEntryZone zone = new ModelEntryZone("bad", List.of("made_up_feature>=1"));
            EntryZone parsed = parser.parse(zone);

            assertThat(parsed.conditions()).isEmpty();
        }

        @Test
        @DisplayName("조건이 없으면 빈 존을 만든다")
        void shouldFailClosedWhenNoConditions() {
            assertThat(parser.parse(null).conditions()).isEmpty();
            assertThat(parser.parse(new ModelEntryZone("d", List.of())).conditions()).isEmpty();
        }
    }

    private static ModelEntryZone champion() {
        return new ModelEntryZone(
                "RSI14 50 상향돌파 & MACD_histogram>0 & rank_turnover>=0.5",
                List.of("rsi_14>=50", "prev_rsi_14<50", "macd_histogram>0", "rank_turnover>=0.5"));
    }
}
