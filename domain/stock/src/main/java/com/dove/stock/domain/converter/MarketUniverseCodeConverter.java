package com.dove.stock.domain.converter;

import com.dove.stock.domain.enums.MarketUniverse;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * MarketUniverse를 명시 코드 TINYINT로 저장한다. 코드 안정성을 위해 ordinal이 아닌 명시 매핑(KRX=0, KONEX=1, NXT=2, INTEGRATED=3)을 쓴다.
 */
@Converter
public class MarketUniverseCodeConverter implements AttributeConverter<MarketUniverse, Byte> {

    @Override
    public Byte convertToDatabaseColumn(MarketUniverse v) {
        if (v == null) return null;
        return switch (v) {
            case KRX -> (byte) 0;
            case KONEX -> (byte) 1;
            case NXT -> (byte) 2;
            case INTEGRATED -> (byte) 3;
        };
    }

    @Override
    public MarketUniverse convertToEntityAttribute(Byte code) {
        if (code == null) return null;
        return switch (code) {
            case 0 -> MarketUniverse.KRX;
            case 1 -> MarketUniverse.KONEX;
            case 2 -> MarketUniverse.NXT;
            case 3 -> MarketUniverse.INTEGRATED;
            default -> throw new IllegalArgumentException("알 수 없는 MarketUniverse 코드: " + code);
        };
    }
}
