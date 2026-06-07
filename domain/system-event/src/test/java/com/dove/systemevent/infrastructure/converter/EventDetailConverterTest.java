package com.dove.systemevent.infrastructure.converter;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("EventDetailConverter")
class EventDetailConverterTest {

    private final EventDetailConverter converter = new EventDetailConverter();

    @Test
    @DisplayName("Map ↔ JSON 왕복이 보존된다")
    void shouldRoundTrip() {
        Map<String, String> detail = new LinkedHashMap<>();
        detail.put("source", "INVESTOR");
        detail.put("error", "타임아웃");

        String json = converter.convertToDatabaseColumn(detail);
        Map<String, String> back = converter.convertToEntityAttribute(json);

        assertThat(back).isEqualTo(detail);
    }

    @Test
    @DisplayName("null·빈 맵은 \"{}\"로 직렬화한다")
    void shouldSerializeNullOrEmptyToEmptyObject() {
        assertThat(converter.convertToDatabaseColumn(null)).isEqualTo("{}");
        assertThat(converter.convertToDatabaseColumn(Map.of())).isEqualTo("{}");
    }

    @Test
    @DisplayName("null·공백 컬럼은 빈 맵으로 역직렬화한다")
    void shouldDeserializeNullOrBlankToEmptyMap() {
        assertThat(converter.convertToEntityAttribute(null)).isEmpty();
        assertThat(converter.convertToEntityAttribute("  ")).isEmpty();
        assertThat(converter.convertToEntityAttribute("{}")).isEmpty();
    }
}
