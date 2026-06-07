package com.dove.stock.application.service;

import com.dove.stock.domain.entity.StockEvent;
import com.dove.stock.domain.enums.StockEventType;
import com.dove.stock.domain.repository.StockEventRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * StockEventService 통합 테스트.
 */
@DataJpaTest
@Import(StockEventService.class)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class StockEventServiceTest {

    @Autowired StockEventService service;
    @Autowired StockEventRepository repository;

    private static final String TICKER = "005930";
    private static final StockEventType TYPE = StockEventType.DIVIDEND;
    private static final LocalDate DATE = LocalDate.of(2024, 3, 20);

    @AfterEach
    void cleanUp() {
        repository.deleteAll();
    }

    @Nested
    @DisplayName("saveIfAbsent — 멱등 저장")
    class SaveIfAbsent {

        @Test
        @DisplayName("이벤트가 없으면 저장한다")
        void shouldSaveWhenAbsent() {
            service.saveIfAbsent(TICKER, TYPE, DATE, "배당 500원", null);

            assertThat(repository.count()).isEqualTo(1);
            assertThat(repository.findAll().get(0).getTicker()).isEqualTo(TICKER);
        }

        @Test
        @DisplayName("동일 ticker·type·date가 이미 있으면 저장하지 않는다")
        void shouldSkipWhenAlreadyExists() {
            service.saveIfAbsent(TICKER, TYPE, DATE, "배당 500원", null);

            service.saveIfAbsent(TICKER, TYPE, DATE, "배당 600원", null);

            assertThat(repository.count()).isEqualTo(1);
        }

        @Test
        @DisplayName("ticker가 null이면 저장하지 않는다")
        void shouldSkipWhenTickerIsNull() {
            service.saveIfAbsent(null, TYPE, DATE, "배당", null);

            assertThat(repository.count()).isZero();
        }

        @Test
        @DisplayName("ticker가 빈 문자열이면 저장하지 않는다")
        void shouldSkipWhenTickerIsBlank() {
            service.saveIfAbsent("  ", TYPE, DATE, "배당", null);

            assertThat(repository.count()).isZero();
        }

        @Test
        @DisplayName("eventDate가 null이면 저장하지 않는다")
        void shouldSkipWhenEventDateIsNull() {
            service.saveIfAbsent(TICKER, TYPE, null, "배당", null);

            assertThat(repository.count()).isZero();
        }
    }

    @Nested
    @DisplayName("findByTicker — 최신순 조회")
    class FindByTicker {

        @Test
        @DisplayName("이벤트를 이벤트일 내림차순으로 반환한다")
        void shouldReturnEventsDescendingByDateWhenFound() {
            service.saveIfAbsent(TICKER, TYPE, LocalDate.of(2024, 1, 10), null, null);
            service.saveIfAbsent(TICKER, TYPE, LocalDate.of(2024, 3, 20), null, null);
            service.saveIfAbsent(TICKER, TYPE, LocalDate.of(2024, 2, 15), null, null);

            List<StockEvent> result = service.findByTicker(TICKER);

            assertThat(result).extracting(StockEvent::getEventDate)
                    .containsExactly(
                            LocalDate.of(2024, 3, 20),
                            LocalDate.of(2024, 2, 15),
                            LocalDate.of(2024, 1, 10));
        }

        @Test
        @DisplayName("해당 ticker의 이벤트가 없으면 빈 목록을 반환한다")
        void shouldReturnEmptyWhenNoEventsForTicker() {
            service.saveIfAbsent("000660", TYPE, DATE, null, null);

            List<StockEvent> result = service.findByTicker(TICKER);

            assertThat(result).isEmpty();
        }
    }
}
