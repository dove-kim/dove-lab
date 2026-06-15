package com.dove.screening.domain.enums;

import com.dove.market.domain.enums.MarketType;
import com.dove.stock.domain.enums.StockExchange;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("FilterVenue")
class FilterVenueTest {

    @Test
    @DisplayName("KRX는 선택 시장별 거래소로 해석한다")
    void krxResolvesToPerMarketExchanges() {
        assertThat(FilterVenue.KRX.resolveExchanges(List.of(MarketType.KOSPI, MarketType.KOSDAQ)))
                .containsExactly(StockExchange.KOSPI, StockExchange.KOSDAQ);
    }

    @Test
    @DisplayName("NXT는 시장과 무관하게 NXT 거래소로 해석한다")
    void nxtResolvesToNxt() {
        assertThat(FilterVenue.NXT.resolveExchanges(List.of(MarketType.KOSPI)))
                .containsExactly(StockExchange.NXT);
    }

    @Test
    @DisplayName("INTEGRATED는 INTEGRATED 거래소로 해석한다")
    void integratedResolvesToIntegrated() {
        assertThat(FilterVenue.INTEGRATED.resolveExchanges(List.of(MarketType.KOSPI, MarketType.KOSDAQ)))
                .containsExactly(StockExchange.INTEGRATED);
    }
}
