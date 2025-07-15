package com.dpot.commonjpa.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.time.LocalDate;
import java.time.ZoneId;

@Converter
public class LocalDateToTimestampConverter implements AttributeConverter<LocalDate, Long> {

    @Override
    public Long convertToDatabaseColumn(LocalDate localDate) {
        if (localDate == null) {
            return null;
        }
        return localDate.atStartOfDay(ZoneId.systemDefault())
                .toInstant()
                .getEpochSecond();
    }

    @Override
    public LocalDate convertToEntityAttribute(Long timestamp) {
        if (timestamp == null) {
            return null;
        }
        return java.time.Instant.ofEpochSecond(timestamp)
                .atZone(ZoneId.systemDefault())
                .toLocalDate();
    }
}


