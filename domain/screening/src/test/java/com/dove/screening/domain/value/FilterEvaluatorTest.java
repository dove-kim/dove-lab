package com.dove.screening.domain.value;

import com.dove.indicator.domain.enums.IndicatorType;
import com.dove.market.domain.enums.MarketType;
import com.dove.stock.domain.entity.StockPrice;
import com.dove.stock.domain.enums.PriceType;
import com.dove.stock.domain.enums.StockExchange;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.EnumMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class FilterEvaluatorTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    /** RSI_14·SMA_5(1000)·SMA_20(900) 지표와 close/volume·시장을 담은 평가 컨텍스트. */
    private EvalContext ctx(double rsi, long close, long volume, MarketType market) {
        Map<IndicatorType, Double> indicators = new EnumMap<>(IndicatorType.class);
        indicators.put(IndicatorType.RSI_14, rsi);
        indicators.put(IndicatorType.SMA_5, 1000.0);
        indicators.put(IndicatorType.SMA_20, 900.0);
        StockPrice price = new StockPrice("005930", StockExchange.KOSPI, PriceType.RAW,
                LocalDate.of(2026, 6, 5), close, close + 50, close - 50, close, volume, 0L);
        return new EvalContext(market, indicators, price);
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
}
