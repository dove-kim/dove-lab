package com.dove.stock.domain.repository;

import com.dove.market.domain.enums.MarketType;
import com.dove.stock.domain.entity.Stock;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class StockRepositoryTest {

    @Autowired
    private StockRepository repository;

    private static final LocalDate LISTING_DATE = LocalDate.of(2000, 1, 2);

    @Test
    @DisplayName("findAllById_MarketType — 시장별 종목 반환")
    void shouldFindAllByMarketType() {
        repository.save(new Stock(MarketType.KOSPI, "005930", "삼성전자", LISTING_DATE, null));
        repository.save(new Stock(MarketType.KOSPI, "000660", "SK하이닉스", LISTING_DATE, null));
        repository.save(new Stock(MarketType.KOSDAQ, "035420", "네이버", LISTING_DATE, null));

        List<Stock> kospi = repository.findAllById_MarketType(MarketType.KOSPI);

        assertThat(kospi).hasSize(2);
        assertThat(kospi).extracting(s -> s.getId().getCode())
                .containsExactlyInAnyOrder("005930", "000660");
    }

    @Test
    @DisplayName("findByMarketTypeAndCode — 특정 종목 조회")
    void shouldFindByMarketTypeAndCode() {
        repository.save(new Stock(MarketType.KOSPI, "005930", "삼성전자", LISTING_DATE, "KR7005930003"));

        Optional<Stock> result = repository.findByMarketTypeAndCode(MarketType.KOSPI, "005930");

        assertThat(result).isPresent();
        assertThat(result.get().getName()).isEqualTo("삼성전자");
        assertThat(result.get().getIsinCode()).isEqualTo("KR7005930003");
    }

    @Test
    @DisplayName("findByMarketTypeAndCode — 없으면 empty 반환")
    void shouldReturnEmptyWhenNotFound() {
        Optional<Stock> result = repository.findByMarketTypeAndCode(MarketType.KOSPI, "999999");

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("findAllByCodeIn — 코드 목록으로 종목 조회")
    void shouldFindAllByCodeIn() {
        repository.save(new Stock(MarketType.KOSPI, "005930", "삼성전자", LISTING_DATE, null));
        repository.save(new Stock(MarketType.KOSDAQ, "035720", "카카오", LISTING_DATE, null));
        repository.save(new Stock(MarketType.KOSPI, "000660", "SK하이닉스", LISTING_DATE, null));

        List<Stock> result = repository.findAllByCodeIn(List.of("005930", "035720"));

        assertThat(result).hasSize(2);
        assertThat(result).extracting(s -> s.getId().getCode())
                .containsExactlyInAnyOrder("005930", "035720");
    }
}
