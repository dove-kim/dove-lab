package com.dove.stock.domain.repository;

import com.dove.market.domain.enums.MarketType;
import com.dove.stock.domain.entity.StockListedDate;
import com.dove.stock.domain.entity.StockListedDateId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class StockListedDateRepositoryTest {

    @Autowired
    private StockListedDateRepository repository;

    @Test
    @DisplayName("저장 후 existsById로 존재 여부 조회")
    void shouldCheckExistenceByCompositeId() {
        StockListedDateId id = new StockListedDateId(MarketType.KOSPI, "005930", LocalDate.of(2026, 4, 17));
        repository.save(new StockListedDate(id));

        assertThat(repository.existsById(id)).isTrue();
        assertThat(repository.existsById(
                new StockListedDateId(MarketType.KOSPI, "005930", LocalDate.of(2026, 4, 18)))
        ).isFalse();
    }

    @Test
    @DisplayName("특정 (market, date) 모든 행 조회 — 벌크 insert 차집합용")
    void shouldFindAllByMarketTypeAndDate() {
        LocalDate date = LocalDate.of(2026, 4, 17);
        repository.save(new StockListedDate(new StockListedDateId(MarketType.KOSPI, "005930", date)));
        repository.save(new StockListedDate(new StockListedDateId(MarketType.KOSPI, "000660", date)));
        repository.save(new StockListedDate(new StockListedDateId(MarketType.KOSDAQ, "035420", date)));

        List<StockListedDate> result = repository.findAllById_MarketTypeAndId_Date(MarketType.KOSPI, date);

        assertThat(result).hasSize(2);
        assertThat(result).extracting(r -> r.getId().getCode())
                .containsExactlyInAnyOrder("005930", "000660");
    }
}
