package com.dove.krx.infrastructure.client;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("KrxListedStockItem")
class KrxListedStockItemTest {

    @Nested
    @DisplayName("getListingDate")
    class GetListingDate {

        @Test
        @DisplayName("shouldReturnLocalDateWhenValidYyyyMmDdString")
        void shouldReturnLocalDateWhenValidYyyyMmDdString() {
            KrxListedStockItem item = itemWithListingDate("19750611");

            assertThat(item.getListingDate()).isEqualTo(LocalDate.of(1975, 6, 11));
        }

        @Test
        @DisplayName("shouldReturnNullWhenDateStringIsNull")
        void shouldReturnNullWhenDateStringIsNull() {
            KrxListedStockItem item = itemWithListingDate(null);

            assertThat(item.getListingDate()).isNull();
        }

        @Test
        @DisplayName("shouldReturnNullWhenDateStringIsBlank")
        void shouldReturnNullWhenDateStringIsBlank() {
            KrxListedStockItem item = itemWithListingDate("   ");

            assertThat(item.getListingDate()).isNull();
        }

        @Test
        @DisplayName("shouldReturnNullWhenDateStringIsInvalidFormat")
        void shouldReturnNullWhenDateStringIsInvalidFormat() {
            KrxListedStockItem item = itemWithListingDate("2024-01-15");

            assertThat(item.getListingDate()).isNull();
        }

        private KrxListedStockItem itemWithListingDate(String listingDateStr) {
            return new KrxListedStockItem(
                    "005930", "KR7005930003", "삼성전자", null, null,
                    listingDateStr, null, "주권", null, "보통주", null, null
            );
        }
    }
}
