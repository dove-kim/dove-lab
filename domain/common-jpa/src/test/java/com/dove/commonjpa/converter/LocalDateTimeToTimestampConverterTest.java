package com.dove.commonjpa.converter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("LocalDateTimeToTimestampConverter 테스트")
class LocalDateTimeToTimestampConverterTest {

    private LocalDateTimeToTimestampConverter converter;

    @BeforeEach
    void setUp() {
        converter = new LocalDateTimeToTimestampConverter();
    }

    @Test
    @DisplayName("LocalDateTime을 epoch seconds로 변환한다")
    void convertToDatabaseColumn_validDateTime() {
        LocalDateTime dateTime = LocalDateTime.of(2023, 10, 26, 14, 30, 0);
        long expected = dateTime.atZone(ZoneId.systemDefault())
                .toInstant()
                .getEpochSecond();

        Long result = converter.convertToDatabaseColumn(dateTime);

        assertThat(result).isEqualTo(expected);
    }

    @Test
    @DisplayName("null LocalDateTime은 null을 반환한다")
    void convertToDatabaseColumn_null() {
        assertThat(converter.convertToDatabaseColumn(null)).isNull();
    }

    @Test
    @DisplayName("epoch seconds를 LocalDateTime으로 변환한다")
    void convertToEntityAttribute_validTimestamp() {
        LocalDateTime expected = LocalDateTime.of(2023, 10, 26, 14, 30, 0);
        long timestamp = expected.atZone(ZoneId.systemDefault())
                .toInstant()
                .getEpochSecond();

        LocalDateTime result = converter.convertToEntityAttribute(timestamp);

        assertThat(result).isEqualTo(expected);
    }

    @Test
    @DisplayName("null timestamp는 null을 반환한다")
    void convertToEntityAttribute_null() {
        assertThat(converter.convertToEntityAttribute(null)).isNull();
    }

    @Test
    @DisplayName("변환 왕복 테스트: LocalDateTime → Long → LocalDateTime")
    void roundTrip() {
        LocalDateTime original = LocalDateTime.of(2024, 1, 15, 9, 0, 30);

        Long timestamp = converter.convertToDatabaseColumn(original);
        LocalDateTime restored = converter.convertToEntityAttribute(timestamp);

        assertThat(restored).isEqualTo(original);
    }

    @Test
    @DisplayName("자정 시각 변환")
    void convertToDatabaseColumn_midnight() {
        LocalDateTime midnight = LocalDateTime.of(2023, 6, 1, 0, 0, 0);
        long expected = midnight.atZone(ZoneId.systemDefault())
                .toInstant()
                .getEpochSecond();

        assertThat(converter.convertToDatabaseColumn(midnight)).isEqualTo(expected);
    }
}
