package com.dove.portfolio.domain.converter;

import com.dove.portfolio.domain.value.RebalancePlanEntry;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.Collections;
import java.util.List;

/**
 * PORTFOLIO_REBALANCE_PLAN.ENTRIES JSON ↔ List&lt;RebalancePlanEntry&gt; 변환기.
 */
@Converter
public class RebalancePlanEntryListConverter implements AttributeConverter<List<RebalancePlanEntry>, String> {

    private static final ObjectMapper MAPPER = com.dove.jpa.JsonSupport.MAPPER;
    private static final TypeReference<List<RebalancePlanEntry>> TYPE = new TypeReference<>() {};

    @Override
    public String convertToDatabaseColumn(List<RebalancePlanEntry> attribute) {
        if (attribute == null || attribute.isEmpty()) return "[]";
        try {
            return MAPPER.writeValueAsString(attribute);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("RebalancePlanEntry 직렬화 실패", e);
        }
    }

    @Override
    public List<RebalancePlanEntry> convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) return Collections.emptyList();
        try {
            return MAPPER.readValue(dbData, TYPE);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("RebalancePlanEntry 역직렬화 실패: " + dbData, e);
        }
    }
}
