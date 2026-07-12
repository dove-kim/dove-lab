package com.dove.screening.domain.pipeline;

import com.dove.jpa.JsonSupport;
import com.dove.screening.domain.value.FilterModel;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.List;

/**
 * 검색 파이프라인 JSON(순서 단계 배열)을 타입 모델로 파싱한다. 모든 파이프라인 JSON 키를 이 클래스 한 곳에 격리한다.
 */
public final class SearchPipeline {

    private SearchPipeline() {
    }

    /**
     * 원본 JSON 문자열을 파이프라인 단계 목록으로 파싱한다. null·빈 문자열·파싱 실패 시 빈 목록.
     */
    public static List<PipelineStage> parse(String json) {
        if (json == null || json.isBlank()) return List.of();
        try {
            return parse(JsonSupport.MAPPER.readTree(json));
        } catch (Exception e) {
            return List.of();
        }
    }

    /**
     * JSON 배열 노드를 파이프라인 단계 목록으로 파싱한다. null이거나 배열이 아니면 빈 목록.
     */
    public static List<PipelineStage> parse(JsonNode array) {
        if (array == null || !array.isArray()) return List.of();
        List<PipelineStage> stages = new ArrayList<>();
        for (JsonNode node : array) {
            PipelineStage stage = parseStage(node);
            if (stage != null) stages.add(stage);
        }
        return stages;
    }

    private static PipelineStage parseStage(JsonNode node) {
        String type = node.path("type").asText();
        return switch (type) {
            case "FILTER" -> parseFilter(node);
            case "RANK" -> parseRank(node);
            default -> null; // 알 수 없는 단계 종류는 건너뛴다
        };
    }

    private static PipelineStage parseFilter(JsonNode node) {
        JsonNode expression = node.path("expression");
        if (expression.isMissingNode() || expression.isNull()) return null;
        return new FilterStage(FilterModel.parse(expression));
    }

    private static PipelineStage parseRank(JsonNode node) {
        List<SortKey> sortKeys = new ArrayList<>();
        for (JsonNode key : node.path("sort")) {
            SortField field = SortField.parseOrNull(key.path("field").asText());
            SortDirection direction = SortDirection.parseOrNull(key.path("direction").asText());
            if (field != null && direction != null) sortKeys.add(new SortKey(field, direction));
        }
        Integer limit = node.hasNonNull("limit") ? node.path("limit").asInt() : null;
        return new RankStage(sortKeys, limit);
    }
}
