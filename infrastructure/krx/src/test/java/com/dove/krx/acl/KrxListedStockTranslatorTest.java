package com.dove.krx.acl;

import com.dove.krx.StockListing;
import com.dove.krx.infrastructure.client.KrxListedStockItem;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("KrxListedStockTranslator")
class KrxListedStockTranslatorTest {

    @Nested
    @DisplayName("translate")
    class Translate {

        @Test
        @DisplayName("shouldMapAllFieldsFromItem")
        void shouldMapAllFieldsFromItem() {
            KrxListedStockItem item = new KrxListedStockItem(
                    "005930", "KR7005930003", "삼성전자", null, null,
                    "19750611", null, "주권", null, "보통주", null, null
            );

            StockListing result = KrxListedStockTranslator.translate(item);

            assertThat(result.ticker()).isEqualTo("005930");
            assertThat(result.isin()).isEqualTo("KR7005930003");
            assertThat(result.listingDate()).isEqualTo(LocalDate.of(1975, 6, 11));
            assertThat(result.secugrpNm()).isEqualTo("주권");
            assertThat(result.kindStkCertTpNm()).isEqualTo("보통주");
        }

        @Test
        @DisplayName("shouldTrimTickerWhitespace")
        void shouldTrimTickerWhitespace() {
            KrxListedStockItem item = new KrxListedStockItem(
                    "  005930  ", "KR7005930003", "삼성전자", null, null,
                    "19750611", null, "주권", null, "보통주", null, null
            );

            StockListing result = KrxListedStockTranslator.translate(item);

            assertThat(result.ticker()).isEqualTo("005930");
        }
    }
}
