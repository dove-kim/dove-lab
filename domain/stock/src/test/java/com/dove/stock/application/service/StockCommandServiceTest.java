package com.dove.stock.application.service;

import com.dove.market.domain.enums.MarketType;
import com.dove.stock.domain.entity.Stock;
import com.dove.stock.domain.repository.StockRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class StockCommandServiceTest {

    @Autowired
    private StockRepository stockRepository;

    private StockCommandService commandService;

    private static final LocalDate LISTING_DATE = LocalDate.of(2000, 1, 2);

    @BeforeEach
    void setUp() {
        commandService = new StockCommandService(stockRepository);
    }

    @Test
    @DisplayName("save — 종목 저장 후 조회 가능")
    void shouldSaveStock() {
        Stock stock = new Stock(MarketType.KOSPI, "005930", "삼성전자", LISTING_DATE, null);

        commandService.save(stock);

        assertThat(stockRepository.findAllById_MarketType(MarketType.KOSPI)).hasSize(1);
    }

    @Test
    @DisplayName("saveAll — 여러 종목 일괄 저장")
    void shouldSaveAllStocks() {
        List<Stock> stocks = List.of(
                new Stock(MarketType.KOSPI, "005930", "삼성전자", LISTING_DATE, null),
                new Stock(MarketType.KOSDAQ, "035420", "네이버", LISTING_DATE, null)
        );

        List<Stock> saved = commandService.saveAll(stocks);

        assertThat(saved).hasSize(2);
        assertThat(stockRepository.count()).isEqualTo(2);
    }

    @Test
    @DisplayName("save — 이름 업데이트")
    void shouldUpdateStockName() {
        Stock stock = new Stock(MarketType.KOSPI, "005930", "삼성전자", LISTING_DATE, null);
        commandService.save(stock);

        stock.updateName("삼성전자(수정)");
        commandService.save(stock);

        Stock updated = stockRepository.findAllById_MarketType(MarketType.KOSPI).get(0);
        assertThat(updated.getName()).isEqualTo("삼성전자(수정)");
    }
}
