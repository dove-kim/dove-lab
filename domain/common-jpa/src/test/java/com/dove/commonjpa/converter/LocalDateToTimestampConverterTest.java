package com.dove.commonjpa.converter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("LocalDateToTimestampConverter 테스트")
class LocalDateToTimestampConverterTest {

    private LocalDateToTimestampConverter converter;

    @BeforeEach
    void setUp() {
        converter = new LocalDateToTimestampConverter();
    }

    @Test
    @DisplayName("LocalDate를 epoch seconds로 변환한다")
    void convertToDatabaseColumn_validDate() {
        LocalDate date = LocalDate.of(2023, 10, 26);
        long expected = date.atStartOfDay(ZoneId.systemDefault())
                .toInstant()
                .getEpochSecond();

        Long result = converter.convertToDatabaseColumn(date);

        assertThat(result).isEqualTo(expected);
    }

    @Test
    @DisplayName("null LocalDate는 null을 반환한다")
    void convertToDatabaseColumn_null() {
        assertThat(converter.convertToDatabaseColumn(null)).isNull();
    }

    @Test
    @DisplayName("epoch seconds를 LocalDate로 변환한다")
    void convertToEntityAttribute_validTimestamp() {
        LocalDate expected = LocalDate.of(2023, 10, 26);
        long timestamp = expected.atStartOfDay(ZoneId.systemDefault())
                .toInstant()
                .getEpochSecond();

        LocalDate result = converter.convertToEntityAttribute(timestamp);

        assertThat(result).isEqualTo(expected);
    }

    @Test
    @DisplayName("null timestamp는 null을 반환한다")
    void convertToEntityAttribute_null() {
        assertThat(converter.convertToEntityAttribute(null)).isNull();
    }

    @Test
    @DisplayName("변환 왕복 테스트: LocalDate → Long → LocalDate")
    void roundTrip() {
        LocalDate original = LocalDate.of(2024, 1, 15);

        Long timestamp = converter.convertToDatabaseColumn(original);
        LocalDate restored = converter.convertToEntityAttribute(timestamp);

        assertThat(restored).isEqualTo(original);
    }

    @Test
    @DisplayName("epoch 기준일(1970-01-01) 변환")
    void convertToDatabaseColumn_epochDate() {
        LocalDate epochDate = LocalDate.of(1970, 1, 1);
        long expected = epochDate.atStartOfDay(ZoneId.systemDefault())
                .toInstant()
                .getEpochSecond();

        assertThat(converter.convertToDatabaseColumn(epochDate)).isEqualTo(expected);
    }
}
