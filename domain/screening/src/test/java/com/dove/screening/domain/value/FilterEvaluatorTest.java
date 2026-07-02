package com.dove.screening.domain.value;

import com.dove.indicator.domain.enums.IndicatorType;
import com.dove.indicator.domain.rank.enums.RankType;
import com.dove.market.domain.enums.MarketType;
import com.dove.stock.domain.entity.StockPrice;
import com.dove.stock.domain.enums.PriceType;
import com.dove.stock.domain.enums.StockExchange;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.EnumMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class FilterEvaluatorTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    /** RSI_14·SMA_5(1000)·SMA_20(900) 지표와 close/volume·시장·순위·모델점수·상승비율(0.65)을 담은 평가 컨텍스트. */
    private EvalContext ctx(double rsi, long close, long volume, MarketType market) {
        Map<IndicatorType, Double> indicators = new EnumMap<>(IndicatorType.class);
        indicators.put(IndicatorType.RSI_14, rsi);
        indicators.put(IndicatorType.SMA_5, 1000.0);
        indicators.put(IndicatorType.SMA_20, 900.0);
        StockPrice price = new StockPrice("005930", StockExchange.KOSPI, PriceType.RAW,
                LocalDate.of(2026, 6, 5), close, close + 50, close - 50, close, volume, 0L);
        Map<RankType, Double> ranks = new EnumMap<>(RankType.class);
        ranks.put(RankType.RANK_RSI_14, 0.8);
        Map<Long, Double> modelScores = Map.of(7L, 0.92);
        return new EvalContext(market, indicators, price, ranks, modelScores, 0.65, false, false);
    }

    /** 상승비율이 비어있는(null) 평가 컨텍스트. */
    private EvalContext ctxWithoutBreadth() {
        return new EvalContext(MarketType.KOSPI, new EnumMap<>(IndicatorType.class), null,
                new EnumMap<>(RankType.class), Map.of(), null, false, false);
    }

    /** 거래정지·관리종목 플래그를 지정한 평가 컨텍스트. */
    private EvalContext ctxWithStatus(boolean halted, boolean admin) {
        return new EvalContext(MarketType.KOSPI, new EnumMap<>(IndicatorType.class), null,
                new EnumMap<>(RankType.class), Map.of(), null, halted, admin);
    }

    private boolean eval(String json, EvalContext ctx) {
        try {
            JsonNode node = MAPPER.readTree(json);
            return FilterEvaluator.evaluate(FilterModel.parse(node), ctx);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    @DisplayName("INDICATOR_VALUE — 연산자로 지표값을 비교한다")
    void indicatorValue() {
        String n = "{\"conditionType\":\"INDICATOR_VALUE\",\"indicator\":\"RSI_14\",\"operator\":\"GT\",\"value\":30}";
        assertThat(eval(n, ctx(40, 100, 1000, MarketType.KOSPI))).isTrue();
        assertThat(eval(n, ctx(20, 100, 1000, MarketType.KOSPI))).isFalse();
    }

    @Test
    @DisplayName("컨텍스트에 없는 지표는 false")
    void missingIndicatorIsFalse() {
        String n = "{\"conditionType\":\"INDICATOR_VALUE\",\"indicator\":\"ATR\",\"operator\":\"GT\",\"value\":0}";
        assertThat(eval(n, ctx(40, 100, 1000, MarketType.KOSPI))).isFalse();
    }

    @Test
    @DisplayName("PRICE_RANGE — 경계 포함 범위를 평가한다")
    void priceRange() {
        String n = "{\"conditionType\":\"PRICE_RANGE\",\"priceField\":\"CLOSE\",\"minValue\":100,\"maxValue\":200}";
        assertThat(eval(n, ctx(40, 150, 1000, MarketType.KOSPI))).isTrue();
        assertThat(eval(n, ctx(40, 250, 1000, MarketType.KOSPI))).isFalse();
    }

    @Test
    @DisplayName("INDICATOR_CROSS — 두 지표를 비교한다 (SMA_5 > SMA_20)")
    void indicatorCross() {
        String n = "{\"conditionType\":\"INDICATOR_CROSS\",\"leftIndicator\":\"SMA_5\",\"rightIndicator\":\"SMA_20\",\"operator\":\"GT\"}";
        assertThat(eval(n, ctx(40, 100, 1000, MarketType.KOSPI))).isTrue();
    }

    @Test
    @DisplayName("MARKET_FILTER — 시장 포함 여부")
    void marketFilter() {
        String n = "{\"conditionType\":\"MARKET_FILTER\",\"markets\":[\"KOSPI\",\"KOSDAQ\"]}";
        assertThat(eval(n, ctx(40, 100, 1000, MarketType.KOSPI))).isTrue();
        assertThat(eval(n, ctx(40, 100, 1000, MarketType.KONEX))).isFalse();
    }

    @Test
    @DisplayName("GROUP AND — 모든 자식을 만족해야 true")
    void groupAnd() {
        String n = "{\"nodeType\":\"GROUP\",\"childOps\":[\"AND\"],\"children\":["
                + "{\"conditionType\":\"INDICATOR_VALUE\",\"indicator\":\"RSI_14\",\"operator\":\"GT\",\"value\":30},"
                + "{\"conditionType\":\"PRICE_VALUE\",\"priceField\":\"CLOSE\",\"operator\":\"GT\",\"value\":50}]}";
        assertThat(eval(n, ctx(40, 100, 1000, MarketType.KOSPI))).isTrue();
        assertThat(eval(n, ctx(20, 100, 1000, MarketType.KOSPI))).isFalse();
    }

    @Test
    @DisplayName("GROUP OR — 하나만 만족해도 true")
    void groupOr() {
        String n = "{\"nodeType\":\"GROUP\",\"childOps\":[\"OR\"],\"children\":["
                + "{\"conditionType\":\"INDICATOR_VALUE\",\"indicator\":\"RSI_14\",\"operator\":\"GT\",\"value\":90},"
                + "{\"conditionType\":\"PRICE_VALUE\",\"priceField\":\"CLOSE\",\"operator\":\"GT\",\"value\":50}]}";
        assertThat(eval(n, ctx(20, 100, 1000, MarketType.KOSPI))).isTrue();
    }

    @Test
    @DisplayName("negated — 조건 결과를 반전한다")
    void negated() {
        String n = "{\"negated\":true,\"conditionType\":\"INDICATOR_VALUE\",\"indicator\":\"RSI_14\",\"operator\":\"GT\",\"value\":30}";
        assertThat(eval(n, ctx(40, 100, 1000, MarketType.KOSPI))).isFalse();
    }

    @Nested
    @DisplayName("MODEL_SCORE_VALUE — 모델 점수 비교")
    class ModelScoreValue {

        @Test
        @DisplayName("컨텍스트의 모델 점수를 연산자로 비교한다")
        void comparesModelScore() {
            String pass = "{\"conditionType\":\"MODEL_SCORE_VALUE\",\"modelId\":7,\"operator\":\"GT\",\"value\":0.9}";
            String fail = "{\"conditionType\":\"MODEL_SCORE_VALUE\",\"modelId\":7,\"operator\":\"GT\",\"value\":0.95}";
            assertThat(eval(pass, ctx(40, 100, 1000, MarketType.KOSPI))).isTrue();
            assertThat(eval(fail, ctx(40, 100, 1000, MarketType.KOSPI))).isFalse();
        }

        @Test
        @DisplayName("점수가 없는 모델은 false")
        void missingModelScoreIsFalse() {
            String n = "{\"conditionType\":\"MODEL_SCORE_VALUE\",\"modelId\":99,\"operator\":\"GT\",\"value\":0}";
            assertThat(eval(n, ctx(40, 100, 1000, MarketType.KOSPI))).isFalse();
        }
    }

    @Nested
    @DisplayName("RANK_VALUE / RANK_RANGE — 순위 비교")
    class RankConditions {

        @Test
        @DisplayName("컨텍스트의 순위값을 연산자로 비교한다")
        void comparesRank() {
            String n = "{\"conditionType\":\"RANK_VALUE\",\"rank\":\"RANK_RSI_14\",\"operator\":\"GTE\",\"value\":0.8}";
            assertThat(eval(n, ctx(40, 100, 1000, MarketType.KOSPI))).isTrue();
        }

        @Test
        @DisplayName("순위 범위(경계 포함)를 평가한다")
        void rankRange() {
            String n = "{\"conditionType\":\"RANK_RANGE\",\"rank\":\"RANK_RSI_14\",\"minValue\":0.7,\"maxValue\":0.9}";
            assertThat(eval(n, ctx(40, 100, 1000, MarketType.KOSPI))).isTrue();
        }

        @Test
        @DisplayName("값이 없는 순위는 false")
        void missingRankIsFalse() {
            String n = "{\"conditionType\":\"RANK_VALUE\",\"rank\":\"RANK_TURNOVER\",\"operator\":\"GT\",\"value\":0}";
            assertThat(eval(n, ctx(40, 100, 1000, MarketType.KOSPI))).isFalse();
        }

        @Test
        @DisplayName("알 수 없는 순위 이름은 false")
        void unknownRankIsFalse() {
            String n = "{\"conditionType\":\"RANK_VALUE\",\"rank\":\"NOPE\",\"operator\":\"GT\",\"value\":0}";
            assertThat(eval(n, ctx(40, 100, 1000, MarketType.KOSPI))).isFalse();
        }
    }

    @Nested
    @DisplayName("BREADTH_VALUE / BREADTH_RANGE — 당일 상승비율 비교")
    class BreadthConditions {

        @Test
        @DisplayName("컨텍스트의 상승비율을 연산자로 비교한다")
        void comparesBreadth() {
            String n = "{\"conditionType\":\"BREADTH_VALUE\",\"operator\":\"GT\",\"value\":0.5}";
            assertThat(eval(n, ctx(40, 100, 1000, MarketType.KOSPI))).isTrue();
        }

        @Test
        @DisplayName("상승비율 범위(경계 포함)를 평가한다")
        void breadthRange() {
            String n = "{\"conditionType\":\"BREADTH_RANGE\",\"minValue\":0.6,\"maxValue\":0.7}";
            assertThat(eval(n, ctx(40, 100, 1000, MarketType.KOSPI))).isTrue();
        }

        @Test
        @DisplayName("상승비율이 없으면 false")
        void missingBreadthIsFalse() {
            String n = "{\"conditionType\":\"BREADTH_VALUE\",\"operator\":\"GT\",\"value\":0}";
            assertThat(eval(n, ctxWithoutBreadth())).isFalse();
        }
    }

    @Nested
    @DisplayName("STOCK_STATUS — 거래정지·관리종목 제외")
    class StockStatus {

        @Test
        @DisplayName("거래정지 제외 시 거래정지 종목은 false")
        void blocksHalted() {
            String n = "{\"conditionType\":\"STOCK_STATUS\",\"exclude\":[\"TRADING_HALT\"]}";
            assertThat(eval(n, ctxWithStatus(true, false))).isFalse();
            assertThat(eval(n, ctxWithStatus(false, false))).isTrue();
        }

        @Test
        @DisplayName("거래정지만 제외하면 관리종목은 통과")
        void adminPassesWhenOnlyHaltExcluded() {
            String n = "{\"conditionType\":\"STOCK_STATUS\",\"exclude\":[\"TRADING_HALT\"]}";
            assertThat(eval(n, ctxWithStatus(false, true))).isTrue();
        }

        @Test
        @DisplayName("관리종목 제외 시 관리종목은 false")
        void blocksAdmin() {
            String n = "{\"conditionType\":\"STOCK_STATUS\",\"exclude\":[\"ADMIN_ITEM\"]}";
            assertThat(eval(n, ctxWithStatus(false, true))).isFalse();
            assertThat(eval(n, ctxWithStatus(false, false))).isTrue();
        }

        @Test
        @DisplayName("exclude 없으면 거래정지·관리종목 둘 다 기본 제외")
        void defaultsToExcludeBoth() {
            String n = "{\"conditionType\":\"STOCK_STATUS\"}";
            assertThat(eval(n, ctxWithStatus(true, false))).isFalse();
            assertThat(eval(n, ctxWithStatus(false, true))).isFalse();
            assertThat(eval(n, ctxWithStatus(false, false))).isTrue();
        }
    }
}
