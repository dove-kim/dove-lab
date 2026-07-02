package com.dove.stock.domain.converter;

import com.dove.stock.domain.enums.StockExchange;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * StockExchange 집합을 정렬된 ordinal 코드 CSV로 저장한다(예: "0,1"). 코드 매핑은 StockExchangeCodeConverter와 동일.
 */
@Converter
public class StockExchangeSetCodeConverter implements AttributeConverter<Set<StockExchange>, String> {

    @Override
    public String convertToDatabaseColumn(Set<StockExchange> exchanges) {
        if (exchanges == null || exchanges.isEmpty()) {
            return "";
        }
        return exchanges.stream()
                .map(StockExchange::ordinal)
                .sorted()
                .map(String::valueOf)
                .collect(Collectors.joining(","));
    }

    @Override
    public Set<StockExchange> convertToEntityAttribute(String csv) {
        Set<StockExchange> result = new LinkedHashSet<>();
        if (csv == null || csv.isBlank()) {
            return result;
        }
        StockExchange[] values = StockExchange.values();
        for (String code : csv.split(",")) {
            result.add(values[Integer.parseInt(code.trim())]);
        }
        return result;
    }
}
