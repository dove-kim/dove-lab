package com.dove.screening.domain.converter;

import com.dove.screening.domain.value.NamePatternCondition;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.Collections;
import java.util.List;

/**
 * STOCK_FILTER.NAME_PATTERN_CONDITIONS JSON ↔ List&lt;NamePatternCondition&gt; 변환기.
 */
@Converter
public class NamePatternConditionListConverter implements AttributeConverter<List<NamePatternCondition>, String> {

    private static final ObjectMapper MAPPER = com.dove.jpa.JsonSupport.MAPPER;
    private static final TypeReference<List<NamePatternCondition>> TYPE = new TypeReference<>() {};

    @Override
    public String convertToDatabaseColumn(List<NamePatternCondition> attribute) {
        if (attribute == null || attribute.isEmpty()) return "[]";
        try {
            return MAPPER.writeValueAsString(attribute);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("NamePatternCondition 직렬화 실패", e);
        }
    }

    @Override
    public List<NamePatternCondition> convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) return Collections.emptyList();
        try {
            return MAPPER.readValue(dbData, TYPE);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("NamePatternCondition 역직렬화 실패: " + dbData, e);
        }
    }
}
