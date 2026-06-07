package com.dove.screening.domain.converter;

import com.dove.market.domain.enums.MarketType;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.List;

/**
 * SEARCH_FILTER.MARKETS JSON ↔ List&lt;MarketType&gt; 변환기.
 */
@Converter
public class MarketTypeListConverter implements AttributeConverter<List<MarketType>, String> {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Override
    public String convertToDatabaseColumn(List<MarketType> attribute) {
        if (attribute == null || attribute.isEmpty()) return null;
        try {
            return MAPPER.writeValueAsString(
                    attribute.stream().map(MarketType::name).toList());
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("List<MarketType> to JSON 변환 실패", e);
        }
    }

    @Override
    public List<MarketType> convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) return List.of();
        try {
            List<String> names = MAPPER.readValue(dbData, new TypeReference<>() {});
            return names.stream().map(MarketType::valueOf).toList();
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("JSON to List<MarketType> 변환 실패: " + dbData, e);
        }
    }
}
