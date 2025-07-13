package com.dove.stockkrxdata.global.jpa;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.time.LocalDateTime;
import java.time.ZoneId;

@Converter
public class LocalDateTimeToTimestampConverter implements AttributeConverter<LocalDateTime, Long> {

    @Override
    public Long convertToDatabaseColumn(LocalDateTime localDateTime) {
        if (localDateTime == null) {
            return null;
        }
        return localDateTime.atZone(ZoneId.systemDefault())
                .toInstant()
                .getEpochSecond();
    }

    @Override
    public LocalDateTime convertToEntityAttribute(Long timestamp) {
        if (timestamp == null) {
            return null;
        }
        return java.time.Instant.ofEpochSecond(timestamp)
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime();
    }
}


