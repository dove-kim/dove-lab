package com.dove.screening.domain.converter;

import com.dove.screening.domain.value.FilterExpression;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * SEARCH_FILTER.EXPRESSION TEXT ↔ FilterExpression 변환기.
 */
@Converter
public class FilterExpressionConverter implements AttributeConverter<FilterExpression, String> {

    @Override
    public String convertToDatabaseColumn(FilterExpression attribute) {
        if (attribute == null) return null;
        return attribute.toJson();
    }

    @Override
    public FilterExpression convertToEntityAttribute(String dbData) {
        return FilterExpression.parse(dbData);
    }
}
