package com.dove.stock.domain.converter;

import com.dove.stock.domain.enums.StockExchange;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * StockExchangeSetCodeConverter 왕복·정렬·빈 처리 테스트.
 */
class StockExchangeSetCodeConverterTest {

    private final StockExchangeSetCodeConverter converter = new StockExchangeSetCodeConverter();

    @Nested
    @DisplayName("convertToDatabaseColumn")
    class ToColumn {

        @Test
        @DisplayName("거래소 집합을 ordinal 코드 오름차순 CSV로 저장한다")
        void shouldWriteSortedCsv() {
            Set<StockExchange> exchanges = new LinkedHashSet<>();
            exchanges.add(StockExchange.KOSDAQ);
            exchanges.add(StockExchange.KOSPI);

            assertThat(converter.convertToDatabaseColumn(exchanges)).isEqualTo("0,1");
        }

        @Test
        @DisplayName("null이나 빈 집합이면 빈 문자열로 저장한다")
        void shouldWriteEmptyWhenNullOrEmpty() {
            assertThat(converter.convertToDatabaseColumn(null)).isEmpty();
            assertThat(converter.convertToDatabaseColumn(Set.of())).isEmpty();
        }
    }

    @Nested
    @DisplayName("convertToEntityAttribute")
    class ToEntity {

        @Test
        @DisplayName("CSV를 거래소 집합으로 복원한다")
        void shouldReadCsv() {
            assertThat(converter.convertToEntityAttribute("0,1"))
                    .containsExactlyInAnyOrder(StockExchange.KOSPI, StockExchange.KOSDAQ);
        }

        @Test
        @DisplayName("null이나 빈 문자열이면 빈 집합을 반환한다")
        void shouldReturnEmptyWhenNullOrBlank() {
            assertThat(converter.convertToEntityAttribute(null)).isEmpty();
            assertThat(converter.convertToEntityAttribute("")).isEmpty();
            assertThat(converter.convertToEntityAttribute("  ")).isEmpty();
        }
    }

    @Nested
    @DisplayName("왕복")
    class RoundTrip {

        @Test
        @DisplayName("여러 거래소 집합을 저장 후 복원하면 동일하다")
        void shouldRoundTrip() {
            Set<StockExchange> exchanges = Set.of(
                    StockExchange.KOSPI, StockExchange.KONEX, StockExchange.INTEGRATED);

            String csv = converter.convertToDatabaseColumn(exchanges);

            assertThat(converter.convertToEntityAttribute(csv))
                    .containsExactlyInAnyOrderElementsOf(exchanges);
        }
    }
}
