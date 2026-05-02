package com.dove.market.application.service;

import com.dove.market.domain.enums.MarketType;
import com.dove.market.domain.repository.MarketDataCursorRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(MarketDataCursorCommandService.class)
class MarketDataCursorCommandServiceTest {

    @Autowired
    private MarketDataCursorCommandService commandService;

    @Autowired
    private MarketDataCursorRepository repository;

    @Test
    @DisplayName("커서 없음 → 신규 생성")
    void shouldCreateCursorWhenNotExists() {
        LocalDate date = LocalDate.of(2026, 4, 25);

        commandService.upsert(MarketType.KOSPI, date);

        assertThat(repository.findById(MarketType.KOSPI))
                .isPresent()
                .hasValueSatisfying(c -> assertThat(c.getLastProcessedDate()).isEqualTo(date));
    }

    @Test
    @DisplayName("커서 있음 → 날짜 갱신")
    void shouldUpdateDateWhenCursorExists() {
        LocalDate old = LocalDate.of(2026, 4, 24);
        LocalDate updated = LocalDate.of(2026, 4, 25);
        commandService.upsert(MarketType.KOSPI, old);

        commandService.upsert(MarketType.KOSPI, updated);

        assertThat(repository.findById(MarketType.KOSPI))
                .isPresent()
                .hasValueSatisfying(c -> assertThat(c.getLastProcessedDate()).isEqualTo(updated));
    }

    @Test
    @DisplayName("시장별 독립 관리")
    void shouldManageCursorsIndependentlyPerMarket() {
        LocalDate kospiDate = LocalDate.of(2026, 4, 25);
        LocalDate kosdaqDate = LocalDate.of(2026, 4, 23);

        commandService.upsert(MarketType.KOSPI, kospiDate);
        commandService.upsert(MarketType.KOSDAQ, kosdaqDate);

        assertThat(repository.findById(MarketType.KOSPI).get().getLastProcessedDate()).isEqualTo(kospiDate);
        assertThat(repository.findById(MarketType.KOSDAQ).get().getLastProcessedDate()).isEqualTo(kosdaqDate);
    }
}
