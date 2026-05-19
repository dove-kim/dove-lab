package com.dove.stock.application.service;

import com.dove.market.domain.enums.MarketType;
import com.dove.stock.domain.entity.Stock;
import com.dove.stock.domain.repository.StockRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class StockQueryServiceTest {

    @Mock
    StockRepository stockRepository;

    @InjectMocks
    StockQueryService stockQueryService;

    private static final LocalDate LISTING_DATE = LocalDate.of(2000, 1, 2);

    @Test
    @DisplayName("findByMarketAndCode — 종목 반환")
    void shouldFindByMarketAndCode() {
        Stock stock = new Stock(MarketType.KOSPI, "005930", "삼성전자", LISTING_DATE, null);
        given(stockRepository.findByMarketTypeAndCode(MarketType.KOSPI, "005930")).willReturn(Optional.of(stock));

        Optional<Stock> result = stockQueryService.findByMarketAndCode(MarketType.KOSPI, "005930");

        assertThat(result).isPresent();
        assertThat(result.get().getName()).isEqualTo("삼성전자");
    }

    @Test
    @DisplayName("findAllByMarket — 시장별 전체 반환")
    void shouldFindAllByMarket() {
        List<Stock> stocks = List.of(
                new Stock(MarketType.KOSPI, "005930", "삼성전자", LISTING_DATE, null),
                new Stock(MarketType.KOSPI, "000660", "SK하이닉스", LISTING_DATE, null)
        );
        given(stockRepository.findAllById_MarketType(MarketType.KOSPI)).willReturn(stocks);

        assertThat(stockQueryService.findAllByMarket(MarketType.KOSPI)).hasSize(2);
    }

    @Test
    @DisplayName("findAll — 전체 종목 반환")
    void shouldFindAll() {
        List<Stock> stocks = List.of(
                new Stock(MarketType.KOSPI, "005930", "삼성전자", LISTING_DATE, null),
                new Stock(MarketType.KOSDAQ, "035720", "카카오", LISTING_DATE, null)
        );
        given(stockRepository.findAll()).willReturn(stocks);

        assertThat(stockQueryService.findAll()).hasSize(2);
    }

    @Test
    @DisplayName("findByCodesAsMap — 코드 → 종목 맵 반환")
    void shouldReturnCodesAsMap() {
        Stock samsung = new Stock(MarketType.KOSPI, "005930", "삼성전자", LISTING_DATE, null);
        Stock kakao = new Stock(MarketType.KOSDAQ, "035720", "카카오", LISTING_DATE, null);
        given(stockRepository.findAllByCodeIn(List.of("005930", "035720")))
                .willReturn(List.of(samsung, kakao));

        Map<String, Stock> result = stockQueryService.findByCodesAsMap(List.of("005930", "035720"));

        assertThat(result).containsKeys("005930", "035720");
        assertThat(result.get("005930").getName()).isEqualTo("삼성전자");
    }

    @Test
    @DisplayName("findByCodesAsMap — 빈 코드 목록이면 빈 맵 반환")
    void shouldReturnEmptyMapWhenCodesEmpty() {
        Map<String, Stock> result = stockQueryService.findByCodesAsMap(List.of());

        assertThat(result).isEmpty();
    }
}
