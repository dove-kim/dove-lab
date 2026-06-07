package com.dove.stock.domain.converter;

import com.dove.stock.domain.enums.StockExchange;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * StockExchange를 ordinal TINYINT 코드로 저장한다. 코드 안정성을 위해 enum 값은 끝에만 추가한다.
 */
@Converter
public class StockExchangeCodeConverter implements AttributeConverter<StockExchange, Byte> {

    @Override
    public Byte convertToDatabaseColumn(StockExchange v) {
        return v == null ? null : (byte) v.ordinal();
    }

    @Override
    public StockExchange convertToEntityAttribute(Byte code) {
        return code == null ? null : StockExchange.values()[code];
    }
}
