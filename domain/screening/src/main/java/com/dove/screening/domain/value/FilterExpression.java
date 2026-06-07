package com.dove.screening.domain.value;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.NullNode;

/**
 * 지표 필터 표현식 트리.
 *
 * @param root 필터 표현식 JSON 트리
 */
public record FilterExpression(JsonNode root) {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    /**
     * 유효한 JSON 문자열에서 FilterExpression을 생성한다.
     *
     * @throws IllegalArgumentException JSON 파싱 실패 시
     */
    public static FilterExpression parse(String json) {
        if (json == null || json.isBlank()) return empty();
        try {
            return new FilterExpression(MAPPER.readTree(json));
        } catch (Exception e) {
            throw new IllegalArgumentException("잘못된 필터 표현식 JSON: " + e.getMessage(), e);
        }
    }

    /**
     * 이미 파싱된 JsonNode에서 FilterExpression을 생성한다.
     */
    public static FilterExpression of(JsonNode node) {
        return new FilterExpression(node != null ? node : NullNode.getInstance());
    }

    /**
     * 빈 표현식 (노드 없음).
     */
    public static FilterExpression empty() {
        return new FilterExpression(NullNode.getInstance());
    }

    /**
     * DB 저장용 JSON 문자열로 직렬화한다.
     *
     * @throws IllegalStateException 직렬화 실패 시
     */
    public String toJson() {
        try {
            return MAPPER.writeValueAsString(root);
        } catch (Exception e) {
            throw new IllegalStateException("FilterExpression 직렬화 실패", e);
        }
    }
}
