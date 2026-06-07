package com.dove.jpa.converter;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("StringListConverter")
class StringListConverterTest {

    private final StringListConverter converter = new StringListConverter();

    @Nested
    @DisplayName("convertToDatabaseColumn")
    class ToColumn {

        @Test
        @DisplayName("값이 있으면 JSON 배열 문자열로 직렬화한다")
        void shouldSerializeToJsonWhenNotEmpty() {
            assertThat(converter.convertToDatabaseColumn(List.of("a", "b")))
                    .isEqualTo("[\"a\",\"b\"]");
        }

        @Test
        @DisplayName("null이면 null을 반환한다")
        void shouldReturnNullWhenNull() {
            assertThat(converter.convertToDatabaseColumn(null)).isNull();
        }

        @Test
        @DisplayName("빈 리스트면 null을 반환한다")
        void shouldReturnNullWhenEmpty() {
            assertThat(converter.convertToDatabaseColumn(List.of())).isNull();
        }
    }

    @Nested
    @DisplayName("convertToEntityAttribute")
    class ToAttribute {

        @Test
        @DisplayName("JSON 배열 문자열을 리스트로 역직렬화한다")
        void shouldDeserializeFromJson() {
            assertThat(converter.convertToEntityAttribute("[\"a\",\"b\"]"))
                    .containsExactly("a", "b");
        }

        @Test
        @DisplayName("null이면 빈 리스트를 반환한다")
        void shouldReturnEmptyWhenNull() {
            assertThat(converter.convertToEntityAttribute(null)).isEmpty();
        }

        @Test
        @DisplayName("공백 문자열이면 빈 리스트를 반환한다")
        void shouldReturnEmptyWhenBlank() {
            assertThat(converter.convertToEntityAttribute("  ")).isEmpty();
        }
    }

    @Test
    @DisplayName("직렬화 후 역직렬화하면 원본과 같다")
    void shouldRoundTrip() {
        List<String> original = List.of("RSI", "MACD", "BOLLINGER");
        String column = converter.convertToDatabaseColumn(original);

        assertThat(converter.convertToEntityAttribute(column)).isEqualTo(original);
    }
}
