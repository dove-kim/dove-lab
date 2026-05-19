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
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(MarketTradingDateService.class)
class MarketTradingDateServiceTest {

    @Autowired MarketTradingDateService service;
    @Autowired MarketTradingDateRepository repository;

    static final MarketType MARKET = MarketType.KOSPI;
    static final LocalDate DATE = LocalDate.of(2026, 4, 25);

    // ── upsert ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("최초 저장 — 없을 때 신규 생성")
    void shouldCreateWhenNotExists() {
        service.upsert(MARKET, DATE, true);

        assertThat(repository.findById(new MarketTradingDateId(MARKET, DATE)))
                .isPresent()
                .hasValueSatisfying(mtd -> assertThat(mtd.isOpen()).isTrue());
    }

    @Test
    @DisplayName("closed → open 업데이트 허용")
    void shouldAllowClosedToOpenUpdate() {
        service.upsert(MARKET, DATE, false);
        service.upsert(MARKET, DATE, true);

        assertThat(repository.findById(new MarketTradingDateId(MARKET, DATE))
                .map(MarketTradingDate::isOpen)).contains(true);
    }

    @Test
    @DisplayName("open → closed 업데이트 금지 — 개장 확정 후 되돌리지 않는다")
    void shouldNotDowngradeFromOpenToClosed() {
        service.upsert(MARKET, DATE, true);
        service.upsert(MARKET, DATE, false);

        assertThat(repository.findById(new MarketTradingDateId(MARKET, DATE))
                .map(MarketTradingDate::isOpen)).contains(true);
    }

    @Test
    @DisplayName("시장별 독립 관리")
    void shouldManageDatesIndependentlyPerMarket() {
        service.upsert(MarketType.KOSPI, DATE, true);
        service.upsert(MarketType.KOSDAQ, DATE, false);

        assertThat(repository.findById(new MarketTradingDateId(MarketType.KOSPI, DATE))
                .map(MarketTradingDate::isOpen)).contains(true);
        assertThat(repository.findById(new MarketTradingDateId(MarketType.KOSDAQ, DATE))
                .map(MarketTradingDate::isOpen)).contains(false);
    }

    // ── findLastProcessedDate ─────────────────────────────────────────────────

    @Test
    @DisplayName("MTD 없음 → empty 반환")
    void shouldReturnEmptyWhenNoMtdRecord() {
        assertThat(service.findLastProcessedDate(MARKET)).isEmpty();
    }

    @Test
    @DisplayName("MTD 있음 → 가장 최근 날짜 반환")
    void shouldReturnLatestDateFromMtd() {
        LocalDate d1 = DATE.minusDays(2);
        LocalDate d2 = DATE.minusDays(1);
        LocalDate d3 = DATE;
        service.upsert(MARKET, d1, true);
        service.upsert(MARKET, d2, false);
        service.upsert(MARKET, d3, true);

        assertThat(service.findLastProcessedDate(MARKET)).hasValue(d3);
    }

    @Test
    @DisplayName("다른 시장 MTD는 영향 없음")
    void shouldNotReturnDateForDifferentMarket() {
        service.upsert(MarketType.KOSDAQ, DATE, true);

        assertThat(service.findLastProcessedDate(MarketType.KOSPI)).isEmpty();
    }

    @Test
    @DisplayName("OPEN/CLOSED 관계없이 가장 최근 날짜 반환")
    void shouldReturnLatestDateRegardlessOfOpenStatus() {
        LocalDate yesterday = DATE.minusDays(1);
        service.upsert(MARKET, yesterday, true);
        service.upsert(MARKET, DATE, false); // CLOSED여도 커서 전진

        assertThat(service.findLastProcessedDate(MARKET)).hasValue(DATE);
    }

    // ── existsOpenDay / existsTradingDateRecord ───────────────────────────────

    @Test
    @DisplayName("existsOpenDay — 개장일이면 true")
    void shouldReturnTrueForOpenDay() {
        service.upsert(MARKET, DATE, true);

        assertThat(service.existsOpenDay(MARKET, DATE)).isTrue();
    }

    @Test
    @DisplayName("existsOpenDay — 휴장일이면 false")
    void shouldReturnFalseForClosedDay() {
        service.upsert(MARKET, DATE, false);

        assertThat(service.existsOpenDay(MARKET, DATE)).isFalse();
    }

    @Test
    @DisplayName("existsOpenDay — 레코드 없으면 false")
    void shouldReturnFalseWhenNoRecord() {
        assertThat(service.existsOpenDay(MARKET, DATE)).isFalse();
    }

    @Test
    @DisplayName("existsTradingDateRecord — 레코드 있으면 true")
    void shouldReturnTrueWhenRecordExists() {
        service.upsert(MARKET, DATE, false);

        assertThat(service.existsTradingDateRecord(MARKET, DATE)).isTrue();
    }

    @Test
    @DisplayName("existsTradingDateRecord — 레코드 없으면 false")
    void shouldReturnFalseWhenRecordNotExists() {
        assertThat(service.existsTradingDateRecord(MARKET, DATE)).isFalse();
    }

    // ── findOpenDatesInRange ──────────────────────────────────────────────────

    @Test
    @DisplayName("기간 내 개장일만 반환")
    void shouldReturnOnlyOpenDatesInRange() {
        LocalDate d1 = DATE;
        LocalDate d2 = DATE.plusDays(1);
        LocalDate d3 = DATE.plusDays(2);
        service.upsert(MARKET, d1, true);
        service.upsert(MARKET, d2, false);
        service.upsert(MARKET, d3, true);

        List<LocalDate> result = service.findOpenDatesInRange(MARKET, d1, d3);

        assertThat(result).containsExactlyInAnyOrder(d1, d3);
    }

    // ── findStatusesInRange ───────────────────────────────────────────────────

    @Test
    @DisplayName("기간 내 개장/휴장 상태 맵 반환")
    void shouldReturnStatusMapInRange() {
        service.upsert(MARKET, DATE, true);
        service.upsert(MARKET, DATE.plusDays(1), false);

        var statuses = service.findStatusesInRange(MARKET, DATE, DATE.plusDays(1));

        assertThat(statuses).containsEntry(DATE, true)
                            .containsEntry(DATE.plusDays(1), false);
    }
}
