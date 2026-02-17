package com.dove.stockkrxdata.enums;

import com.dove.stockdata.enums.MarketType;
import com.dove.stockkrxdata.client.KrxStockClient;
import com.dove.stockkrxdata.client.KrxStockResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.time.LocalDate;
import java.util.function.BiFunction;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

@DisplayName("KrxMarketType 테스트")
class KrxMarketTypeTest {

    @Test
    @DisplayName("MarketType.KOSPI → KrxMarketType.KOSPI 변환")
    void of_kospi() {
        assertThat(KrxMarketType.of(MarketType.KOSPI)).isEqualTo(KrxMarketType.KOSPI);
    }

    @Test
    @DisplayName("MarketType.KOSDAQ → KrxMarketType.KOSDAQ 변환")
    void of_kosdaq() {
        assertThat(KrxMarketType.of(MarketType.KOSDAQ)).isEqualTo(KrxMarketType.KOSDAQ);
    }

    @Test
    @DisplayName("MarketType.KONEX → KrxMarketType.KONEX 변환")
    void of_konex() {
        assertThat(KrxMarketType.of(MarketType.KONEX)).isEqualTo(KrxMarketType.KONEX);
    }

    @Test
    @DisplayName("KOSPI.toMarketType() → MarketType.KOSPI")
    void toMarketType_kospi() {
        assertThat(KrxMarketType.KOSPI.toMarketType()).isEqualTo(MarketType.KOSPI);
    }

    @Test
    @DisplayName("KOSDAQ.toMarketType() → MarketType.KOSDAQ")
    void toMarketType_kosdaq() {
        assertThat(KrxMarketType.KOSDAQ.toMarketType()).isEqualTo(MarketType.KOSDAQ);
    }

    @Test
    @DisplayName("KONEX.toMarketType() → MarketType.KONEX")
    void toMarketType_konex() {
        assertThat(KrxMarketType.KONEX.toMarketType()).isEqualTo(MarketType.KONEX);
    }

    @ParameterizedTest
    @EnumSource(MarketType.class)
    @DisplayName("모든 MarketType에 대해 of → toMarketType 왕복 변환이 성립한다")
    void roundTrip_allMarketTypes(MarketType marketType) {
        KrxMarketType krxType = KrxMarketType.of(marketType);
        assertThat(krxType.toMarketType()).isEqualTo(marketType);
    }

    @Test
    @DisplayName("KONEX getClientMethod 호출 시 UnsupportedOperationException 발생")
    void getClientMethod_konex_throwsException() {
        KrxStockClient mockClient = mock(KrxStockClient.class);
        BiFunction<String, LocalDate, KrxStockResponse> method = KrxMarketType.KONEX.getClientMethod(mockClient);

        assertThatThrownBy(() -> method.apply("authKey", LocalDate.now()))
                .isInstanceOf(UnsupportedOperationException.class)
                .hasMessageContaining("KONEX");
    }

    @Test
    @DisplayName("KOSPI getClientMethod는 null이 아닌 BiFunction을 반환한다")
    void getClientMethod_kospi_returnsNonNull() {
        KrxStockClient mockClient = mock(KrxStockClient.class);
        assertThat(KrxMarketType.KOSPI.getClientMethod(mockClient)).isNotNull();
    }

    @Test
    @DisplayName("KOSDAQ getClientMethod는 null이 아닌 BiFunction을 반환한다")
    void getClientMethod_kosdaq_returnsNonNull() {
        KrxStockClient mockClient = mock(KrxStockClient.class);
        assertThat(KrxMarketType.KOSDAQ.getClientMethod(mockClient)).isNotNull();
    }
}
