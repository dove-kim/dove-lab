package com.dove.modelserving.domain.meta;

import com.dove.modelserving.application.exception.InvalidModelMetaException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * meta.json 문자열을 ModelMeta로 파싱하는 파서.
 */
@Component
public class ModelMetaParser {

    private final ObjectMapper objectMapper;

    public ModelMetaParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * meta.json 문자열을 파싱한다. JSON 형식이 잘못됐으면 거부한다.
     *
     * @throws InvalidModelMetaException JSON 파싱 실패 시
     */
    public ModelMeta parse(String metaJson) {
        JsonNode root;
        try {
            root = objectMapper.readTree(metaJson);
        } catch (Exception e) {
            throw new InvalidModelMetaException("INVALID_META_JSON");
        }
        if (root == null || !root.isObject()) {
            throw new InvalidModelMetaException("INVALID_META_JSON");
        }
        return new ModelMeta(
                text(root, "name"),
                text(root, "version"),
                text(root, "output_type"),
                stringList(root.get("features")),
                text(root, "feature_hash"),
                entryZone(root.get("entry_zone")));
    }

    private static String text(JsonNode node, String field) {
        JsonNode v = node.get(field);
        return v == null || v.isNull() ? null : v.asText();
    }

    private static List<String> stringList(JsonNode arrayNode) {
        if (arrayNode == null || !arrayNode.isArray()) return null;
        List<String> values = new ArrayList<>(arrayNode.size());
        for (JsonNode element : arrayNode) {
            values.add(element.asText());
        }
        return values;
    }

    private static ModelEntryZone entryZone(JsonNode zoneNode) {
        if (zoneNode == null || !zoneNode.isObject()) return null;
        return new ModelEntryZone(text(zoneNode, "desc"), stringList(zoneNode.get("conditions")));
    }
}
