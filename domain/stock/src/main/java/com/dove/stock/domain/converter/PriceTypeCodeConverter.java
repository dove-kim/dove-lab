package com.dove.stock.domain.converter;

import com.dove.stock.domain.enums.PriceType;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * PriceType을 ordinal TINYINT 코드로 저장한다. 코드 안정성을 위해 enum 값은 끝에만 추가한다.
 */
@Converter
public class PriceTypeCodeConverter implements AttributeConverter<PriceType, Byte> {

    @Override
    public Byte convertToDatabaseColumn(PriceType v) {
        return v == null ? null : (byte) v.ordinal();
    }

    @Override
    public PriceType convertToEntityAttribute(Byte code) {
        return code == null ? null : PriceType.values()[code];
    }
}
