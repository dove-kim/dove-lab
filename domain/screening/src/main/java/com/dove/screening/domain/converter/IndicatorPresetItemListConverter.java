package com.dove.screening.domain.converter;

import com.dove.screening.domain.value.IndicatorPresetItem;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.Collections;
import java.util.List;

/**
 * INDICATOR_PRESET.ITEMS JSON ↔ List&lt;IndicatorPresetItem&gt; 변환기.
 */
@Converter
public class IndicatorPresetItemListConverter
        implements AttributeConverter<List<IndicatorPresetItem>, String> {

    private static final ObjectMapper MAPPER = com.dove.jpa.JsonSupport.MAPPER;
    private static final TypeReference<List<IndicatorPresetItem>> TYPE = new TypeReference<>() {};

    @Override
    public String convertToDatabaseColumn(List<IndicatorPresetItem> attribute) {
        if (attribute == null || attribute.isEmpty()) return "[]";
        try {
            return MAPPER.writeValueAsString(attribute);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("IndicatorPresetItem 직렬화 실패", e);
        }
    }

    @Override
    public List<IndicatorPresetItem> convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) return Collections.emptyList();
        try {
            return MAPPER.readValue(dbData, TYPE);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("IndicatorPresetItem 역직렬화 실패: " + dbData, e);
        }
    }
}
