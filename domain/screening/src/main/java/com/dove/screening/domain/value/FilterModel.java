package com.dove.screening.domain.value;

import com.dove.indicator.domain.enums.IndicatorType;
import com.dove.indicator.domain.rank.enums.RankType;
import com.dove.market.domain.enums.MarketType;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * 검색식 JSON 트리를 타입 모델(FilterNode)로 파싱한다. 모든 JSON 키 문자열을 이 클래스 한 곳에 격리한다.
 */
public final class FilterModel {

    private FilterModel() {
    }

    /**
     * JSON 트리를 FilterNode 모델로 변환한다. 해석할 수 없는 leaf는 UnknownCondition으로 표현한다.
     */
    public static FilterNode parse(JsonNode node) {
        FilterNode base = FilterNodes.isGroup(node) ? parseGroup(node) : parseCondition(node);
        return FilterNodes.negated(node) ? new FilterNot(base) : base;
    }

    private static FilterNode parseGroup(JsonNode node) {
        List<FilterNode> children = new ArrayList<>();
        for (JsonNode child : node.path("children")) children.add(parse(child));
        List<FilterChildOp> ops = new ArrayList<>();
        for (int i = 1; i < children.size(); i++) ops.add(FilterNodes.childOp(node, i));
        return new FilterGroup(children, ops);
    }

    private static FilterNode parseCondition(JsonNode n) {
        FilterConditionType type = FilterConditionType.parseOrNull(n.path("conditionType").asText());
        if (type == null) return new UnknownCondition();
        return switch (type) {
            case INDICATOR_VALUE -> threshold(indicatorOperand(n.path("indicator").asText(), offset(n)), n);
            case INDICATOR_RANGE -> range(indicatorOperand(n.path("indicator").asText(), offset(n)), n);
            case INDICATOR_CROSS -> comparison(indicatorOperand(n.path("leftIndicator").asText(), leftOffset(n)),
                    indicatorOperand(n.path("rightIndicator").asText(), rightOffset(n)), n);
            case PRICE_VALUE -> threshold(priceOperand(n.path("priceField").asText(), offset(n)), n);
            case PRICE_RANGE -> range(priceOperand(n.path("priceField").asText(), offset(n)), n);
            case VOLUME_VALUE -> threshold(new VolumeOperand(offset(n)), n);
            case VOLUME_RANGE -> range(new VolumeOperand(offset(n)), n);
            case TURNOVER_VALUE -> threshold(new TurnoverOperand(offset(n)), n);
            case TURNOVER_RANGE -> range(new TurnoverOperand(offset(n)), n);
            case PRICE_VS_INDICATOR -> comparison(priceOperand(n.path("priceField").asText(), leftOffset(n)),
                    indicatorOperand(n.path("indicator").asText(), rightOffset(n)), n);
            case MARKET_FILTER -> marketFilter(n);
            case MODEL_SCORE_VALUE -> threshold(new ModelScoreOperand(n.path("modelId").asLong(), offset(n)), n);
            case MODEL_SCORE_RANGE -> range(new ModelScoreOperand(n.path("modelId").asLong(), offset(n)), n);
            case RANK_VALUE -> threshold(rankOperand(n.path("rank").asText(), offset(n)), n);
            case RANK_RANGE -> range(rankOperand(n.path("rank").asText(), offset(n)), n);
            case CUSTOM_METRIC_VALUE -> threshold(new CustomMetricOperand(n.path("metricId").asLong(), offset(n)), n);
            case CUSTOM_METRIC_RANGE -> range(new CustomMetricOperand(n.path("metricId").asLong(), offset(n)), n);
            case STOCK_STATUS -> stockStatus(n);
        };
    }

    private static FilterNode stockStatus(JsonNode n) {
        Set<StockStatusType> exclude = EnumSet.noneOf(StockStatusType.class);
        for (JsonNode e : n.path("exclude")) {
            StockStatusType t = StockStatusType.parseOrNull(e.asText());
            if (t != null) exclude.add(t);
        }
        // 비어있거나 없으면 둘 다 제외가 기본
        if (exclude.isEmpty()) {
            exclude = EnumSet.allOf(StockStatusType.class);
        }
        return new StockStatusCondition(exclude);
    }

    private static FilterNode threshold(FilterOperand operand, JsonNode n) {
        FilterOperator op = FilterOperator.parseOrNull(n.path("operator").asText());
        if (operand == null || op == null) return new UnknownCondition();
        return new ThresholdCondition(operand, op, n.path("value").asDouble());
    }

    private static FilterNode range(FilterOperand operand, JsonNode n) {
        if (operand == null) return new UnknownCondition();
        return new RangeCondition(operand, new FilterRange(
                n.path("minValue").asDouble(), n.path("maxValue").asDouble(),
                n.path("minInclusive").asBoolean(true), n.path("maxInclusive").asBoolean(true)));
    }

    private static FilterNode comparison(FilterOperand left, FilterOperand right, JsonNode n) {
        FilterOperator op = FilterOperator.parseOrNull(n.path("operator").asText());
        if (left == null || right == null || op == null) return new UnknownCondition();
        return new ComparisonCondition(left, op, right);
    }

    private static FilterNode marketFilter(JsonNode n) {
        List<MarketType> markets = new ArrayList<>();
        for (JsonNode m : n.path("markets")) {
            try {
                markets.add(MarketType.valueOf(m.asText()));
            } catch (IllegalArgumentException ignored) {
                // 알 수 없는 시장은 무시
            }
        }
        return new MarketFilterCondition(markets);
    }

    private static FilterOperand indicatorOperand(String name, int offset) {
        IndicatorType t = IndicatorType.parseOrNull(name);
        return t == null ? null : new IndicatorOperand(t, offset);
    }

    private static FilterOperand priceOperand(String field, int offset) {
        FilterPriceField f = FilterPriceField.parseOrNull(field);
        return f == null ? null : new PriceOperand(f, offset);
    }

    private static FilterOperand rankOperand(String name, int offset) {
        RankType t = RankType.parseOrNull(name);
        return t == null ? null : new RankOperand(t, offset);
    }

    /** 거래일 오프셋 상한 — 약 1년치 거래일. */
    private static final int MAX_OFFSET = 250;

    private static int offset(JsonNode n) {
        return clampOffset(n.path("offset").asInt(0));
    }

    private static int leftOffset(JsonNode n) {
        return clampOffset(n.path("leftOffset").asInt(0));
    }

    private static int rightOffset(JsonNode n) {
        return clampOffset(n.path("rightOffset").asInt(0));
    }

    private static int clampOffset(int v) {
        return Math.max(-MAX_OFFSET, Math.min(MAX_OFFSET, v));
    }
}
