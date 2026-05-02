package com.dove.market.application.service;

import com.dove.market.domain.entity.MarketDataCursor;
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
@Import(MarketDataCursorQueryService.class)
class MarketDataCursorQueryServiceTest {

    @Autowired
    private MarketDataCursorQueryService queryService;

    @Autowired
    private MarketDataCursorRepository repository;

    @Test
    @DisplayName("커서 있음 → lastProcessedDate 반환")
    void shouldReturnLastProcessedDateWhenExists() {
        LocalDate date = LocalDate.of(2026, 4, 25);
        repository.save(new MarketDataCursor(MarketType.KOSPI, date));

        assertThat(queryService.findLastProcessedDate(MarketType.KOSPI))
                .isPresent()
                .hasValue(date);
    }

    @Test
    @DisplayName("커서 없음 → empty 반환")
    void shouldReturnEmptyWhenNotExists() {
        assertThat(queryService.findLastProcessedDate(MarketType.KOSPI)).isEmpty();
    }

    @Test
    @DisplayName("다른 시장 커서는 영향 없음")
    void shouldNotReturnCursorForDifferentMarket() {
        repository.save(new MarketDataCursor(MarketType.KOSDAQ, LocalDate.of(2026, 4, 25)));

        assertThat(queryService.findLastProcessedDate(MarketType.KOSPI)).isEmpty();
    }
}
