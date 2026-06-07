package com.dove.screening.domain.converter;

import com.dove.screening.domain.value.StockCondition;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.Collections;
import java.util.List;

/**
 * STOCK_FILTER.STOCK_CONDITIONS JSON ↔ List&lt;StockCondition&gt; 변환기.
 */
@Converter
public class StockConditionListConverter implements AttributeConverter<List<StockCondition>, String> {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final TypeReference<List<StockCondition>> TYPE = new TypeReference<>() {};

    @Override
    public String convertToDatabaseColumn(List<StockCondition> attribute) {
        if (attribute == null || attribute.isEmpty()) return "[]";
        try {
            return MAPPER.writeValueAsString(attribute);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("StockCondition 직렬화 실패", e);
        }
    }

    @Override
    public List<StockCondition> convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) return Collections.emptyList();
        try {
            return MAPPER.readValue(dbData, TYPE);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("StockCondition 역직렬화 실패: " + dbData, e);
        }
    }
}
