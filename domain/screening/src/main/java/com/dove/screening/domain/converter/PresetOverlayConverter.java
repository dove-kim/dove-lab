package com.dove.screening.domain.converter;

import com.dove.screening.domain.value.PresetOverlay;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * INDICATOR_PRESET.OVERLAY JSON ↔ PresetOverlay 변환기.
 */
@Converter
public class PresetOverlayConverter implements AttributeConverter<PresetOverlay, String> {

    private static final ObjectMapper MAPPER = com.dove.jpa.JsonSupport.MAPPER;

    @Override
    public String convertToDatabaseColumn(PresetOverlay attribute) {
        if (attribute == null) return null;
        try {
            return MAPPER.writeValueAsString(attribute);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("PresetOverlay 직렬화 실패", e);
        }
    }

    @Override
    public PresetOverlay convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) return null;
        try {
            return MAPPER.readValue(dbData, PresetOverlay.class);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("PresetOverlay 역직렬화 실패: " + dbData, e);
        }
    }
}
