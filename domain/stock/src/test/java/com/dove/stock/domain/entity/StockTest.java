package com.dove.stock.domain.entity;

import com.dove.market.domain.enums.MarketType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class StockTest {

    private static final LocalDate LISTING_DATE = LocalDate.of(2000, 1, 2);

    @Test
    @DisplayName("생성자 — 필드가 올바르게 설정된다")
    void shouldCreateWithCorrectFields() {
        Stock stock = new Stock(MarketType.KOSPI, "005930", "삼성전자", LISTING_DATE, "KR7005930003");

        assertThat(stock.getId().getMarketType()).isEqualTo(MarketType.KOSPI);
        assertThat(stock.getId().getCode()).isEqualTo("005930");
        assertThat(stock.getName()).isEqualTo("삼성전자");
        assertThat(stock.getListingDate()).isEqualTo(LISTING_DATE);
        assertThat(stock.getIsinCode()).isEqualTo("KR7005930003");
    }

    @Test
    @DisplayName("생성자 — isinCode는 null 허용")
    void shouldAllowNullIsinCode() {
        Stock stock = new Stock(MarketType.KOSPI, "005930", "삼성전자", LISTING_DATE, null);

        assertThat(stock.getIsinCode()).isNull();
    }

    @Test
    @DisplayName("updateName — 이름이 변경된다")
    void shouldUpdateName() {
        Stock stock = new Stock(MarketType.KOSPI, "005930", "삼성전자", LISTING_DATE, null);

        stock.updateName("삼성전자우");

        assertThat(stock.getName()).isEqualTo("삼성전자우");
    }
}
