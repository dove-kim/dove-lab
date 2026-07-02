package com.dove.screening.infrastructure.repository;

import com.dove.indicator.domain.entity.QStockFeatureDaily;
import com.dove.screening.domain.value.FilterModel;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 검색식을 SQL 조건으로 밀어넣을 수 있는지(폴백 경계)를 검증한다. 변환 가능하면 값이, 불가하면 빈 값이 나온다.
 */
class StockFeatureFilterTranslatorTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private Optional<TranslatedFilter> translate(String json) {
        try {
            JsonNode node = MAPPER.readTree(json);
            return StockFeatureFilterTranslator.translate(FilterModel.parse(node), QStockFeatureDaily.stockFeatureDaily);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    @DisplayName("숫자 지표 비교 — SQL 변환 가능")
    void shouldTranslateNumericIndicator() {
        String n = "{\"conditionType\":\"INDICATOR_VALUE\",\"indicator\":\"RSI_14\",\"operator\":\"GT\",\"value\":30}";
        assertThat(translate(n)).isPresent();
    }

    @Test
    @DisplayName("오프셋 지표(N일 전) — SQL 변환 가능 + 오프셋 별칭 생성")
    void shouldTranslateOffsetIndicator() {
        String n = "{\"conditionType\":\"INDICATOR_VALUE\",\"indicator\":\"RSI_14\",\"offset\":-5,\"operator\":\"GT\",\"value\":30}";
        Optional<TranslatedFilter> result = translate(n);
        assertThat(result).isPresent();
        assertThat(result.get().offsetAliases()).containsKey(-5);
    }

    @Test
    @DisplayName("오프셋 교차(오늘 vs N일 전) — 좌우 오프셋 별칭 분리")
    void shouldTranslateCrossWithOffsets() {
        String valid = "{\"conditionType\":\"INDICATOR_CROSS\",\"leftIndicator\":\"SMA_5\",\"leftOffset\":0,"
                + "\"rightIndicator\":\"SMA_5\",\"rightOffset\":-20,\"operator\":\"GT\"}";
        assertThat(translate(valid)).isPresent();
        assertThat(translate(valid).get().offsetAliases()).containsKeys(0, -20);
    }

    @Test
    @DisplayName("가격·거래량·시장·교차 — SQL 변환 가능")
    void shouldTranslateOtherSupportedLeaves() {
        assertThat(translate("{\"conditionType\":\"PRICE_RANGE\",\"priceField\":\"CLOSE\",\"minValue\":100,\"maxValue\":200}")).isPresent();
        assertThat(translate("{\"conditionType\":\"VOLUME_VALUE\",\"operator\":\"GT\",\"value\":1000}")).isPresent();
        assertThat(translate("{\"conditionType\":\"MARKET_FILTER\",\"markets\":[\"KOSPI\",\"KOSDAQ\"]}")).isPresent();
        assertThat(translate("{\"conditionType\":\"INDICATOR_CROSS\",\"leftIndicator\":\"SMA_5\",\"rightIndicator\":\"SMA_20\",\"operator\":\"GT\"}")).isPresent();
    }

    @Test
    @DisplayName("불리언 지표(IS_*) — SQL 미지원 → 폴백")
    void shouldFallbackForBooleanIndicator() {
        String n = "{\"conditionType\":\"INDICATOR_VALUE\",\"indicator\":\"IS_52W_HIGH\",\"operator\":\"EQ\",\"value\":1}";
        assertThat(translate(n)).isEmpty();
    }

    @Test
    @DisplayName("종목 상태(STOCK_STATUS) — SQL 미지원 → 폴백")
    void shouldFallbackForStockStatus() {
        assertThat(translate("{\"conditionType\":\"STOCK_STATUS\",\"exclude\":[\"TRADING_HALT\"]}")).isEmpty();
    }

    @Test
    @DisplayName("알 수 없는 지표·연산자·조건종류 — 폴백")
    void shouldFallbackForUnknownTokens() {
        assertThat(translate("{\"conditionType\":\"INDICATOR_VALUE\",\"indicator\":\"NOPE\",\"operator\":\"GT\",\"value\":1}")).isEmpty();
        assertThat(translate("{\"conditionType\":\"INDICATOR_VALUE\",\"indicator\":\"RSI_14\",\"operator\":\"BAD\",\"value\":1}")).isEmpty();
        assertThat(translate("{\"conditionType\":\"WHAT_IS_THIS\"}")).isEmpty();
    }

    @Test
    @DisplayName("빈 그룹 — 폴백")
    void shouldFallbackForEmptyGroup() {
        assertThat(translate("{\"nodeType\":\"GROUP\",\"children\":[]}")).isEmpty();
    }

    @Test
    @DisplayName("그룹에 미지원 자식이 하나라도 있으면 전체 폴백")
    void shouldFallbackWhenAnyChildUnsupported() {
        String n = "{\"nodeType\":\"GROUP\",\"childOps\":[\"AND\"],\"children\":["
                + "{\"conditionType\":\"INDICATOR_VALUE\",\"indicator\":\"RSI_14\",\"operator\":\"GT\",\"value\":30},"
                + "{\"conditionType\":\"INDICATOR_VALUE\",\"indicator\":\"IS_52W_HIGH\",\"operator\":\"EQ\",\"value\":1}]}";
        assertThat(translate(n)).isEmpty();
    }

    @Test
    @DisplayName("모든 자식이 지원되는 그룹 — SQL 변환 가능")
    void shouldTranslateFullySupportedGroup() {
        String n = "{\"nodeType\":\"GROUP\",\"childOps\":[\"AND\"],\"children\":["
                + "{\"conditionType\":\"INDICATOR_VALUE\",\"indicator\":\"RSI_14\",\"operator\":\"GT\",\"value\":30},"
                + "{\"conditionType\":\"PRICE_VALUE\",\"priceField\":\"CLOSE\",\"operator\":\"GT\",\"value\":50}]}";
        assertThat(translate(n)).isPresent();
    }

    @Test
    @DisplayName("부정(negated) 지원 조건 — SQL 변환 가능")
    void shouldTranslateNegatedSupported() {
        String n = "{\"negated\":true,\"conditionType\":\"INDICATOR_VALUE\",\"indicator\":\"RSI_14\",\"operator\":\"GT\",\"value\":30}";
        assertThat(translate(n)).isPresent();
    }

    @Nested
    @DisplayName("MODEL_SCORE — 모델점수 join 별칭 생성")
    class ModelScore {

        @Test
        @DisplayName("모델점수 비교 — SQL 변환 가능 + 해당 모델 join 별칭 생성")
        void shouldTranslateModelScoreValue() {
            String n = "{\"conditionType\":\"MODEL_SCORE_VALUE\",\"modelId\":7,\"operator\":\"GT\",\"value\":0.9}";
            Optional<TranslatedFilter> result = translate(n);
            assertThat(result).isPresent();
            assertThat(result.get().modelScoreAliases()).hasSize(1);
            assertThat(result.get().modelScoreAliases().get(0).modelId()).isEqualTo(7L);
            assertThat(result.get().modelScoreAliases().get(0).offset()).isZero();
        }

        @Test
        @DisplayName("오프셋 모델점수 — 오프셋 피처 별칭과 모델 join 별칭이 함께 생성")
        void shouldTranslateModelScoreWithOffset() {
            String n = "{\"conditionType\":\"MODEL_SCORE_VALUE\",\"modelId\":3,\"offset\":-5,\"operator\":\"GT\",\"value\":0.5}";
            Optional<TranslatedFilter> result = translate(n);
            assertThat(result).isPresent();
            assertThat(result.get().offsetAliases()).containsKey(-5);
            assertThat(result.get().modelScoreAliases().get(0).offset()).isEqualTo(-5);
        }

        @Test
        @DisplayName("서로 다른 모델 두 개 — 별칭 두 개 생성")
        void shouldCreateDistinctAliasesPerModel() {
            String n = "{\"nodeType\":\"GROUP\",\"childOps\":[\"AND\"],\"children\":["
                    + "{\"conditionType\":\"MODEL_SCORE_VALUE\",\"modelId\":1,\"operator\":\"GT\",\"value\":0.5},"
                    + "{\"conditionType\":\"MODEL_SCORE_VALUE\",\"modelId\":2,\"operator\":\"GT\",\"value\":0.5}]}";
            Optional<TranslatedFilter> result = translate(n);
            assertThat(result).isPresent();
            assertThat(result.get().modelScoreAliases()).hasSize(2);
        }
    }

    @Nested
    @DisplayName("RANK — 순위 join 별칭 생성")
    class Rank {

        @Test
        @DisplayName("순위 비교 — SQL 변환 가능 + 순위 join 별칭 생성")
        void shouldTranslateRankValue() {
            String n = "{\"conditionType\":\"RANK_VALUE\",\"rank\":\"RANK_RSI_14\",\"operator\":\"GTE\",\"value\":0.8}";
            Optional<TranslatedFilter> result = translate(n);
            assertThat(result).isPresent();
            assertThat(result.get().rankAliases()).hasSize(1);
            assertThat(result.get().rankAliases().get(0).offset()).isZero();
        }

        @Test
        @DisplayName("순위 범위 — SQL 변환 가능")
        void shouldTranslateRankRange() {
            String n = "{\"conditionType\":\"RANK_RANGE\",\"rank\":\"RANK_TURNOVER\",\"minValue\":0.7,\"maxValue\":0.9}";
            assertThat(translate(n)).isPresent();
        }

        @Test
        @DisplayName("오프셋 순위 — 오프셋 피처 별칭과 순위 join 별칭이 함께 생성")
        void shouldTranslateRankWithOffset() {
            String n = "{\"conditionType\":\"RANK_VALUE\",\"rank\":\"RANK_RET_5D\",\"offset\":-3,\"operator\":\"GT\",\"value\":0.5}";
            Optional<TranslatedFilter> result = translate(n);
            assertThat(result).isPresent();
            assertThat(result.get().offsetAliases()).containsKey(-3);
            assertThat(result.get().rankAliases().get(0).offset()).isEqualTo(-3);
        }

        @Test
        @DisplayName("알 수 없는 순위 이름 — 폴백")
        void shouldFallbackForUnknownRank() {
            assertThat(translate("{\"conditionType\":\"RANK_VALUE\",\"rank\":\"NOPE\",\"operator\":\"GT\",\"value\":1}")).isEmpty();
        }
    }

    @Nested
    @DisplayName("BREADTH — 당일 상승비율 join 별칭 생성")
    class Breadth {

        @Test
        @DisplayName("상승비율 비교 — SQL 변환 가능 + 상승비율 join 별칭 생성")
        void shouldTranslateBreadthValue() {
            String n = "{\"conditionType\":\"BREADTH_VALUE\",\"operator\":\"GT\",\"value\":0.5}";
            Optional<TranslatedFilter> result = translate(n);
            assertThat(result).isPresent();
            assertThat(result.get().breadthAliases()).hasSize(1);
            assertThat(result.get().breadthAliases().get(0).offset()).isZero();
        }

        @Test
        @DisplayName("상승비율 범위 — SQL 변환 가능")
        void shouldTranslateBreadthRange() {
            String n = "{\"conditionType\":\"BREADTH_RANGE\",\"minValue\":0.6,\"maxValue\":0.7}";
            assertThat(translate(n)).isPresent();
        }

        @Test
        @DisplayName("오프셋 상승비율 — 오프셋 피처 별칭과 상승비율 join 별칭이 함께 생성")
        void shouldTranslateBreadthWithOffset() {
            String n = "{\"conditionType\":\"BREADTH_VALUE\",\"offset\":-3,\"operator\":\"GT\",\"value\":0.5}";
            Optional<TranslatedFilter> result = translate(n);
            assertThat(result).isPresent();
            assertThat(result.get().offsetAliases()).containsKey(-3);
            assertThat(result.get().breadthAliases().get(0).offset()).isEqualTo(-3);
        }
    }
}
