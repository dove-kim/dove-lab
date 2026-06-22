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
    @DisplayName("register")
    class Register {

        @Test
        @DisplayName("레코드 없을 때 거래일을 생성한다")
        void shouldCreateWhenNotExists() {
            service.register(KRX, DATE);

            assertThat(repository.findById(new ExchangeTradingDateId(KRX, DATE))).isPresent();
        }

        @Test
        @DisplayName("이미 있으면 pricesSynced를 되돌리지 않는다 (멱등)")
        void shouldNotResetPricesSyncedWhenAlreadyExists() {
            service.register(KRX, DATE);
            service.markPricesSynced(KRX, DATE);

            service.register(KRX, DATE);

            assertThat(repository.findById(new ExchangeTradingDateId(KRX, DATE))
                    .map(ExchangeTradingDate::isPricesSynced)).contains(true);
        }
    }

    @Nested
    @DisplayName("existsTradingDay")
    class ExistsTradingDay {

        @Test
        @DisplayName("등록된 거래일이면 true를 반환한다")
        void shouldReturnTrueForRegisteredDay() {
            service.register(KRX, DATE);

            assertThat(service.existsTradingDay(KRX, DATE)).isTrue();
        }

        @Test
        @DisplayName("레코드 없으면 false를 반환한다")
        void shouldReturnFalseWhenNoRecord() {
            assertThat(service.existsTradingDay(KRX, DATE)).isFalse();
        }
    }

    @Nested
    @DisplayName("findTradingDatesInRange")
    class FindTradingDatesInRange {

        @Test
        @DisplayName("기간 내 거래일을 반환한다")
        void shouldReturnDatesInRange() {
            LocalDate d1 = DATE;
            LocalDate d3 = DATE.plusDays(2);
            service.register(KRX, d1);
            service.register(KRX, d3);

            List<LocalDate> result = service.findTradingDatesInRange(KRX, d1, d3);

            assertThat(result).containsExactlyInAnyOrder(d1, d3);
        }

        @Test
        @DisplayName("기간 밖 거래일은 제외한다")
        void shouldExcludeDatesOutOfRange() {
            service.register(KRX, DATE.minusDays(1));
            service.register(KRX, DATE);

            List<LocalDate> result = service.findTradingDatesInRange(KRX, DATE, DATE);

            assertThat(result).containsExactly(DATE);
        }
    }

    @Nested
    @DisplayName("findRecentTradingDates")
    class FindRecentTradingDates {

        @Test
        @DisplayName("최근 거래일을 limit개까지 내림차순으로 반환한다")
        void shouldReturnRecentDatesDescWithinLimit() {
            LocalDate d1 = DATE;
            LocalDate d2 = DATE.plusDays(1);
            LocalDate d3 = DATE.plusDays(2);
            LocalDate d4 = DATE.plusDays(3);
            service.register(KRX, d1);
            service.register(KRX, d2);
            service.register(KRX, d3);
            service.register(KRX, d4);

            List<LocalDate> result = service.findRecentTradingDates(KRX, d4, 2);

            assertThat(result).containsExactly(d4, d3);
        }

        @Test
        @DisplayName("onOrBefore 이후 거래일은 제외한다")
        void shouldExcludeDatesAfterOnOrBefore() {
            LocalDate d1 = DATE;
            LocalDate d2 = DATE.plusDays(1);
            LocalDate d3 = DATE.plusDays(2);
            service.register(KRX, d1);
            service.register(KRX, d2);
            service.register(KRX, d3);

            List<LocalDate> result = service.findRecentTradingDates(KRX, d2, 10);

            assertThat(result).containsExactly(d2, d1);
        }
    }

    @Nested
    @DisplayName("findUnsyncedPriceDates")
    class FindUnsyncedPriceDates {

        @Test
        @DisplayName("pricesSynced=false인 날짜만 반환한다")
        void shouldReturnUnsyncedDates() {
            LocalDate d1 = DATE;
            LocalDate d2 = DATE.plusDays(1);
            service.register(KRX, d1);
            service.register(KRX, d2);
            service.markPricesSynced(KRX, d1);

            List<LocalDate> result = service.findUnsyncedPriceDates(KRX, d1, d2);

            assertThat(result).containsExactly(d2);
        }
    }

    @Nested
    @DisplayName("markPricesSynced")
    class MarkPricesSynced {

        @Test
        @DisplayName("pricesSynced를 true로 변경한다")
        void shouldMarkPricesSynced() {
            service.register(KRX, DATE);
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
        @DisplayName("가장 최근 거래일을 반환한다")
        void shouldReturnLatestDate() {
            service.register(KRX, DATE.minusDays(2));
            service.register(KRX, DATE.minusDays(1));
            service.register(KRX, DATE);

            assertThat(service.findLastProcessedDate(KRX)).hasValue(DATE);
        }
    }
}
