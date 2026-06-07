package com.dove.screening.domain.value;

import com.dove.indicator.domain.enums.IndicatorType;
import com.dove.market.domain.enums.MarketType;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.List;

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
            case INDICATOR_VALUE -> threshold(indicatorOperand(n.path("indicator").asText()), n);
            case INDICATOR_RANGE -> range(indicatorOperand(n.path("indicator").asText()), n);
            case INDICATOR_CROSS -> comparison(indicatorOperand(n.path("leftIndicator").asText()),
                    indicatorOperand(n.path("rightIndicator").asText()), n);
            case PRICE_VALUE -> threshold(priceOperand(n.path("priceField").asText()), n);
            case PRICE_RANGE -> range(priceOperand(n.path("priceField").asText()), n);
            case VOLUME_VALUE -> threshold(new VolumeOperand(), n);
            case VOLUME_RANGE -> range(new VolumeOperand(), n);
            case PRICE_VS_INDICATOR -> comparison(priceOperand(n.path("priceField").asText()),
                    indicatorOperand(n.path("indicator").asText()), n);
            case MARKET_FILTER -> marketFilter(n);
        };
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

    private static FilterOperand indicatorOperand(String name) {
        IndicatorType t = IndicatorType.parseOrNull(name);
        return t == null ? null : new IndicatorOperand(t);
    }

    private static FilterOperand priceOperand(String field) {
        FilterPriceField f = FilterPriceField.parseOrNull(field);
        return f == null ? null : new PriceOperand(f);
    }
}
