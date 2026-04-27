package com.dove.stock.domain.entity;

import com.dove.market.domain.enums.MarketType;
import com.dove.stock.domain.enums.TradingStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class StockTest {

    @Test
    @DisplayName("생성 시 tradingStatus를 지정할 수 있다")
    void shouldCreateWithTradingStatus() {
        Stock stock = new Stock(MarketType.KOSPI, "005930", "삼성전자", TradingStatus.ACTIVE);

        assertThat(stock.getTradingStatus()).isEqualTo(TradingStatus.ACTIVE);
    }

    @Test
    @DisplayName("updateTradingStatus로 상태를 변경할 수 있다")
    void shouldUpdateTradingStatus() {
        Stock stock = new Stock(MarketType.KOSPI, "005930", "삼성전자", TradingStatus.ACTIVE);

        stock.updateTradingStatus(TradingStatus.SUSPENDED);

        assertThat(stock.getTradingStatus()).isEqualTo(TradingStatus.SUSPENDED);
    }
}
