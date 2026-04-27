package com.dove.stock.domain.repository;

import com.dove.market.domain.enums.MarketType;
import com.dove.stock.domain.entity.Stock;
import com.dove.stock.domain.enums.TradingStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class StockRepositoryTest {

    @Autowired
    private StockRepository repository;

    @Test
    @DisplayName("findAllByTradingStatus — ACTIVE만 반환, SUSPENDED/DELISTED 제외")
    void shouldFindOnlyActiveStocks() {
        repository.save(new Stock(MarketType.KOSPI, "005930", "삼성전자", TradingStatus.ACTIVE));
        repository.save(new Stock(MarketType.KOSPI, "000660", "SK하이닉스", TradingStatus.SUSPENDED));
        repository.save(new Stock(MarketType.KOSDAQ, "035420", "네이버", TradingStatus.DELISTED));
        repository.save(new Stock(MarketType.KOSDAQ, "035720", "카카오", TradingStatus.ACTIVE));

        List<Stock> active = repository.findAllByTradingStatus(TradingStatus.ACTIVE);

        assertThat(active).hasSize(2);
        assertThat(active).extracting(s -> s.getId().getCode())
                .containsExactlyInAnyOrder("005930", "035720");
    }

    @Test
    @DisplayName("findAllByTradingStatusAndId_MarketType — 시장 필터 적용")
    void shouldFilterByTradingStatusAndMarket() {
        repository.save(new Stock(MarketType.KOSPI, "005930", "삼성전자", TradingStatus.ACTIVE));
        repository.save(new Stock(MarketType.KOSDAQ, "035720", "카카오", TradingStatus.ACTIVE));
        repository.save(new Stock(MarketType.KOSPI, "000660", "SK하이닉스", TradingStatus.SUSPENDED));

        List<Stock> activeKospi = repository.findAllByTradingStatusAndId_MarketType(
                TradingStatus.ACTIVE, MarketType.KOSPI);

        assertThat(activeKospi).hasSize(1);
        assertThat(activeKospi.get(0).getId().getCode()).isEqualTo("005930");
    }
}
