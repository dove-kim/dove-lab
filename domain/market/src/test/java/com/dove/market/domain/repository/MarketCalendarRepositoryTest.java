package com.dove.market.domain.repository;

import com.dove.market.domain.entity.MarketCalendar;
import com.dove.market.domain.enums.MarketDayType;
import com.dove.market.domain.enums.MarketType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class MarketCalendarRepositoryTest {

    @Autowired
    private MarketCalendarRepository repository;

    @Test
    @DisplayName("findFirstByMarketTypeAndDayTypeOrderByDateDesc — 최신 TRADING 조회")
    void shouldFindLatestTrading() {
        repository.save(new MarketCalendar(LocalDate.of(2026, 4, 13), MarketType.KOSPI, MarketDayType.TRADING));
        repository.save(new MarketCalendar(LocalDate.of(2026, 4, 14), MarketType.KOSPI, MarketDayType.TRADING));
        repository.save(new MarketCalendar(LocalDate.of(2026, 4, 15), MarketType.KOSPI, MarketDayType.HOLIDAY));

        Optional<MarketCalendar> result = repository
                .findFirstByMarketTypeAndDayTypeOrderByDateDesc(MarketType.KOSPI, MarketDayType.TRADING);

        assertThat(result).isPresent();
        assertThat(result.get().getDate()).isEqualTo(LocalDate.of(2026, 4, 14));
    }

    @Test
    @DisplayName("시장별 독립")
    void shouldFilterByMarketType() {
        repository.save(new MarketCalendar(LocalDate.of(2026, 4, 14), MarketType.KOSPI, MarketDayType.TRADING));
        repository.save(new MarketCalendar(LocalDate.of(2026, 4, 15), MarketType.KOSDAQ, MarketDayType.TRADING));

        Optional<MarketCalendar> result = repository
                .findFirstByMarketTypeAndDayTypeOrderByDateDesc(MarketType.KOSPI, MarketDayType.TRADING);

        assertThat(result).isPresent();
        assertThat(result.get().getDate()).isEqualTo(LocalDate.of(2026, 4, 14));
    }
}
