package com.dove.screening.domain.value;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * 검색식 트리 노드의 구조(그룹 여부·부정·자식 결합 연산자)를 해석하는 헬퍼.
 */
public final class FilterNodes {

    private static final String LOGIC_NOT = "NOT";

    private FilterNodes() {
    }

    /**
     * 노드가 그룹(자식들을 결합하는 노드)인지 여부를 반환한다.
     */
    public static boolean isGroup(JsonNode node) {
        return "GROUP".equals(node.path("nodeType").asText());
    }

    /**
     * 노드가 부정(NOT)인지 여부를 반환한다. negated 플래그 우선, 없으면 legacy logic="NOT"으로 판단한다.
     */
    public static boolean negated(JsonNode node) {
        return node.has("negated")
                ? node.path("negated").asBoolean(false)
                : LOGIC_NOT.equals(node.path("logic").asText("AND"));
    }

    /**
     * 그룹의 기본 결합 연산자를 반환한다. legacy logic이 AND/OR이면 그 값을, 그 외엔 AND를 쓴다.
     */
    public static FilterChildOp defaultOp(JsonNode group) {
        if (!group.has("negated")) {
            String logic = group.path("logic").asText("AND");
            if (!LOGIC_NOT.equals(logic)) return FilterChildOp.parseOrDefault(logic, FilterChildOp.AND);
        }
        return FilterChildOp.AND;
    }

    /**
     * 그룹에서 position번째(1부터) 자식에 적용할 결합 연산자를 반환한다. childOps가 없으면 기본 연산자를 쓴다.
     */
    public static FilterChildOp childOp(JsonNode group, int position) {
        FilterChildOp fallback = defaultOp(group);
        JsonNode childOps = group.path("childOps");
        if (position >= 1 && childOps.isArray() && childOps.size() > position - 1) {
            return FilterChildOp.parseOrDefault(childOps.get(position - 1).asText(), fallback);
        }
        return fallback;
    }
}
