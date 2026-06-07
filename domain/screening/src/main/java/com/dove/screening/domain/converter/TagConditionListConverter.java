package com.dove.screening.domain.converter;

import com.dove.screening.domain.value.TagCondition;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.Collections;
import java.util.List;

/**
 * STOCK_FILTER.TAG_CONDITIONS JSON ↔ List&lt;TagCondition&gt; 변환기.
 */
@Converter
public class TagConditionListConverter implements AttributeConverter<List<TagCondition>, String> {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final TypeReference<List<TagCondition>> TYPE = new TypeReference<>() {};

    @Override
    public String convertToDatabaseColumn(List<TagCondition> attribute) {
        if (attribute == null || attribute.isEmpty()) return "[]";
        try {
            return MAPPER.writeValueAsString(attribute);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("TagCondition 직렬화 실패", e);
        }
    }

    @Override
    public List<TagCondition> convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) return Collections.emptyList();
        try {
            return MAPPER.readValue(dbData, TYPE);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("TagCondition 역직렬화 실패: " + dbData, e);
        }
    }
}
