package com.dove.market.application.service;

import com.dove.market.domain.entity.ExchangeTradingDate;
import com.dove.market.domain.entity.ExchangeTradingDateId;
import com.dove.market.domain.enums.Exchange;
import com.dove.market.domain.repository.ExchangeTradingDateRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(ExchangeTradingDateService.class)
@DisplayName("ExchangeTradingDateService")
class ExchangeTradingDateServiceTest {

    @Autowired ExchangeTradingDateService service;
    @Autowired ExchangeTradingDateRepository repository;

    static final Exchange KRX = Exchange.KRX;
    static final LocalDate DATE = LocalDate.of(2026, 4, 25);

    @Nested
    @DisplayName("upsert")
    class Upsert {

        @Test
        @DisplayName("레코드 없을 때 신규 생성한다")
        void shouldCreateWhenNotExists() {
            service.upsert(KRX, DATE, true);

            assertThat(repository.findById(new ExchangeTradingDateId(KRX, DATE)))
                    .isPresent()
                    .hasValueSatisfying(etd -> assertThat(etd.isOpen()).isTrue());
        }

        @Test
        @DisplayName("closed → open 갱신을 허용한다")
        void shouldAllowClosedToOpenUpdate() {
            service.upsert(KRX, DATE, false);
            service.upsert(KRX, DATE, true);

            assertThat(repository.findById(new ExchangeTradingDateId(KRX, DATE))
                    .map(ExchangeTradingDate::isOpen)).contains(true);
        }

        @Test
        @DisplayName("open → closed 갱신은 무시한다 — 개장 확정 후 되돌리지 않는다")
        void shouldNotDowngradeFromOpenToClosed() {
            service.upsert(KRX, DATE, true);
            service.upsert(KRX, DATE, false);

            assertThat(repository.findById(new ExchangeTradingDateId(KRX, DATE))
                    .map(ExchangeTradingDate::isOpen)).contains(true);
        }
    }

    @Nested
    @DisplayName("existsOpenDay")
    class ExistsOpenDay {

        @Test
        @DisplayName("개장일이면 true를 반환한다")
        void shouldReturnTrueForOpenDay() {
            service.upsert(KRX, DATE, true);

            assertThat(service.existsOpenDay(KRX, DATE)).isTrue();
        }

        @Test
        @DisplayName("휴장일이면 false를 반환한다")
        void shouldReturnFalseForClosedDay() {
            service.upsert(KRX, DATE, false);

            assertThat(service.existsOpenDay(KRX, DATE)).isFalse();
        }

        @Test
        @DisplayName("레코드 없으면 false를 반환한다")
        void shouldReturnFalseWhenNoRecord() {
            assertThat(service.existsOpenDay(KRX, DATE)).isFalse();
        }
    }

    @Nested
    @DisplayName("findOpenDatesInRange")
    class FindOpenDatesInRange {

        @Test
        @DisplayName("기간 내 개장일만 반환한다")
        void shouldReturnOnlyOpenDatesInRange() {
            LocalDate d1 = DATE;
            LocalDate d2 = DATE.plusDays(1);
            LocalDate d3 = DATE.plusDays(2);
            service.upsert(KRX, d1, true);
            service.upsert(KRX, d2, false);
            service.upsert(KRX, d3, true);

            List<LocalDate> result = service.findOpenDatesInRange(KRX, d1, d3);

            assertThat(result).containsExactlyInAnyOrder(d1, d3);
        }
    }

    @Nested
    @DisplayName("findUnsyncedPriceDates")
    class FindUnsyncedPriceDates {

        @Test
        @DisplayName("open=true AND pricesSynced=false인 날짜만 반환한다")
        void shouldReturnUnsyncedOpenDates() {
            LocalDate d1 = DATE;
            LocalDate d2 = DATE.plusDays(1);
            service.upsert(KRX, d1, true);
            service.upsert(KRX, d2, true);
            service.markPricesSynced(KRX, d1);

            List<LocalDate> result = service.findUnsyncedPriceDates(KRX, d1, d2);

            assertThat(result).containsExactly(d2);
        }

        @Test
        @DisplayName("휴장일은 미수집 조회에서 제외한다")
        void shouldNotReturnClosedDaysInUnsyncedQuery() {
            service.upsert(KRX, DATE, false);

            assertThat(service.findUnsyncedPriceDates(KRX, DATE, DATE)).isEmpty();
        }
    }

    @Nested
    @DisplayName("markPricesSynced")
    class MarkPricesSynced {

        @Test
        @DisplayName("pricesSynced를 true로 변경한다")
        void shouldMarkPricesSynced() {
            service.upsert(KRX, DATE, true);
            service.markPricesSynced(KRX, DATE);

            assertThat(repository.findById(new ExchangeTradingDateId(KRX, DATE))
                    .map(ExchangeTradingDate::isPricesSynced)).contains(true);
        }

        @Test
        @DisplayName("레코드 없으면 무시한다 (no-op)")
        void shouldIgnoreWhenNoRecord() {
            service.markPricesSynced(KRX, DATE);

            assertThat(repository.findById(new ExchangeTradingDateId(KRX, DATE))).isEmpty();
        }
    }

    @Nested
    @DisplayName("findLastProcessedDate")
    class FindLastProcessedDate {

        @Test
        @DisplayName("레코드 없으면 empty를 반환한다")
        void shouldReturnEmptyWhenNoRecord() {
            assertThat(service.findLastProcessedDate(KRX)).isEmpty();
        }

        @Test
        @DisplayName("가장 최근 날짜를 반환한다")
        void shouldReturnLatestDate() {
            service.upsert(KRX, DATE.minusDays(2), true);
            service.upsert(KRX, DATE.minusDays(1), false);
            service.upsert(KRX, DATE, true);

            assertThat(service.findLastProcessedDate(KRX)).hasValue(DATE);
        }
    }
}
