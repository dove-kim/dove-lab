package com.dove.custommetric.domain.spec;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 커스텀 지표 계산식 JSON을 MetricSpec 트리로 파싱한다. 모든 JSON 키를 이 클래스 한 곳에 격리한다.
 *
 * <p>형식: {@code {"lets": {"이름": <node>}, "root": <node>}}. 각 node는 {@code op} 필드로 종류를 구분한다:
 * agg·const·ref·roll_mean·ema·cumprod1p·lag, 그리고 이항연산(gt·lt·gte·lte·add·sub·mul·div·and·or).
 */
public final class MetricSpecParser {

    private static final ObjectMapper MAPPER = com.dove.jpa.JsonSupport.MAPPER;

    private MetricSpecParser() {
    }

    /**
     * JSON 문자열을 MetricSpec으로 파싱한다.
     *
     * @throws IllegalArgumentException JSON이 잘못됐거나 알 수 없는 연산일 때
     */
    public static MetricSpec parse(String json) {
        try {
            return parse(MAPPER.readTree(json));
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalArgumentException("잘못된 지표 스펙 JSON: " + e.getMessage(), e);
        }
    }

    /**
     * JsonNode를 MetricSpec으로 파싱한다.
     *
     * @throws IllegalArgumentException root가 없거나 알 수 없는 연산일 때
     */
    public static MetricSpec parse(JsonNode node) {
        Map<String, MetricNode> lets = new LinkedHashMap<>();
        JsonNode letsNode = node.path("lets");
        if (letsNode.isObject()) {
            letsNode.fields().forEachRemaining(e -> lets.put(e.getKey(), node(e.getValue())));
        }
        if (!node.hasNonNull("root")) {
            throw new IllegalArgumentException("지표 스펙에 root가 없음");
        }
        return new MetricSpec(lets, node(node.get("root")));
    }

    private static MetricNode node(JsonNode n) {
        String op = n.path("op").asText();
        return switch (op) {
            case "agg" -> new AggNode(
                    MetricAgg.valueOf(n.path("agg").asText()),
                    n.path("colA").asText(),
                    n.hasNonNull("colB") ? n.get("colB").asText() : null,
                    n.path("universeFilterId").asLong());
            case "const" -> new ConstNode(n.path("value").asDouble());
            case "ref" -> new RefNode(n.path("name").asText());
            case "roll_mean" -> new RollMeanNode(node(n.get("input")),
                    n.path("window").asInt(), n.path("minPeriods").asInt());
            case "ema" -> new EmaNode(node(n.get("input")), n.path("window").asInt());
            case "cumprod1p" -> new CumProd1pNode(node(n.get("input")));
            case "lag" -> new LagNode(node(n.get("input")), n.path("periods").asInt());
            case "gt", "lt", "gte", "lte", "add", "sub", "mul", "div", "and", "or" ->
                    new BinaryNode(BinaryOp.valueOf(op.toUpperCase()), node(n.get("left")), node(n.get("right")));
            default -> throw new IllegalArgumentException("알 수 없는 지표 연산: " + op);
        };
    }
}
