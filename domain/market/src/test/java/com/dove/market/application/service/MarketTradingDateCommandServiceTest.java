package com.dove.market.application.service;

import com.dove.market.domain.entity.MarketTradingDate;
import com.dove.market.domain.entity.MarketTradingDateId;
import com.dove.market.domain.enums.MarketType;
import com.dove.market.domain.repository.MarketTradingDateRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(MarketTradingDateCommandService.class)
class MarketTradingDateCommandServiceTest {

    @Autowired
    private MarketTradingDateCommandService commandService;

    @Autowired
    private MarketTradingDateRepository repository;

    private static final MarketType MARKET = MarketType.KOSPI;
    private static final LocalDate DATE = LocalDate.of(2026, 4, 25);

    @Test
    @DisplayName("최초 저장 — 없을 때 신규 생성")
    void shouldCreateWhenNotExists() {
        commandService.upsert(MARKET, DATE, true);

        Optional<MarketTradingDate> found = repository.findById(new MarketTradingDateId(MARKET, DATE));
        assertThat(found).isPresent();
        assertThat(found.get().isOpen()).isTrue();
    }

    @Test
    @DisplayName("closed → open 업데이트 허용")
    void shouldAllowClosedToOpenUpdate() {
        commandService.upsert(MARKET, DATE, false);

        commandService.upsert(MARKET, DATE, true);

        assertThat(repository.findById(new MarketTradingDateId(MARKET, DATE))
                .map(MarketTradingDate::isOpen)).contains(true);
    }

    @Test
    @DisplayName("open → closed 업데이트 금지 — 개장 확정 후 되돌리지 않는다")
    void shouldNotDowngradeFromOpenToClosed() {
        commandService.upsert(MARKET, DATE, true);

        commandService.upsert(MARKET, DATE, false);

        assertThat(repository.findById(new MarketTradingDateId(MARKET, DATE))
                .map(MarketTradingDate::isOpen)).contains(true);
    }

    @Test
    @DisplayName("시장별 독립 관리")
    void shouldManageDatesIndependentlyPerMarket() {
        commandService.upsert(MarketType.KOSPI, DATE, true);
        commandService.upsert(MarketType.KOSDAQ, DATE, false);

        assertThat(repository.findById(new MarketTradingDateId(MarketType.KOSPI, DATE))
                .map(MarketTradingDate::isOpen)).contains(true);
        assertThat(repository.findById(new MarketTradingDateId(MarketType.KOSDAQ, DATE))
                .map(MarketTradingDate::isOpen)).contains(false);
    }
}
