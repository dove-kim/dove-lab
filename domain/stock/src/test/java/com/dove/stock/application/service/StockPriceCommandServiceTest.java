package com.dove.stock.application.service;

import com.dove.stock.domain.entity.StockPrice;
import com.dove.stock.domain.entity.StockPriceId;
import com.dove.stock.domain.enums.PriceType;
import com.dove.stock.domain.enums.StockExchange;
import com.dove.stock.domain.repository.StockPriceRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * StockPriceCommandService 통합 테스트.
 */
@DataJpaTest
@Import(StockPriceCommandService.class)
class StockPriceCommandServiceTest {

    private static final String TICKER = "005930";
    private static final StockExchange EX = StockExchange.KOSPI;
    private static final PriceType PT = PriceType.RAW;
    private static final LocalDate D = LocalDate.of(2024, 5, 30);

    @Autowired StockPriceCommandService service;
    @Autowired StockPriceRepository repository;

    private StockPrice price(long close) {
        return new StockPrice(TICKER, EX, PT, D, 100L, 110L, 90L, close, 1000L, 0L);
    }

    @Nested
    @DisplayName("upsert — 단건 upsert")
    class Upsert {

        @Test
        @DisplayName("존재하지 않으면 insert한다")
        void shouldInsertWhenAbsent() {
            service.upsert(TICKER, EX, PT, D, 100L, 110L, 90L, 70000L, 1000L, 0L);

            assertThat(repository.count()).isEqualTo(1);
        }

        @Test
        @DisplayName("이미 존재하면 값을 갱신하고 행 수를 유지한다")
        void shouldUpdateWhenExists() {
            service.upsert(TICKER, EX, PT, D, 100L, 110L, 90L, 70000L, 1000L, 0L);
            service.upsert(TICKER, EX, PT, D, 100L, 110L, 90L, 75000L, 2000L, 0L);

            assertThat(repository.count()).isEqualTo(1);
            StockPrice saved = repository.findById(new StockPriceId(TICKER, EX, PT, D)).orElseThrow();
            assertThat(saved.getClosePrice()).isEqualTo(75000L);
        }
    }

    @Nested
    @DisplayName("upsertAll — 일괄 upsert")
    class UpsertAll {

        @Test
        @DisplayName("신규는 insert한다")
        void shouldInsertNew() {
            service.upsertAll(List.of(price(70000)));

            assertThat(repository.count()).isEqualTo(1);
        }

        @Test
        @DisplayName("같은 키 재저장 시 값을 갱신하고 행 수를 유지한다")
        void shouldUpdateOnReSave() {
            service.upsertAll(List.of(price(70000)));
            service.upsertAll(List.of(price(75000)));

            assertThat(repository.count()).isEqualTo(1);
            StockPrice saved = repository.findById(new StockPriceId(TICKER, EX, PT, D)).orElseThrow();
            assertThat(saved.getClosePrice()).isEqualTo(75000);
        }

        @Test
        @DisplayName("거래소·가격유형이 다르면 별개 행으로 저장한다")
        void shouldSeparateByExchangeAndPriceType() {
            service.upsertAll(List.of(
                    new StockPrice(TICKER, StockExchange.KOSPI, PriceType.RAW, D, 1L, 1L, 1L, 1L, 1L, 0L),
                    new StockPrice(TICKER, StockExchange.KOSPI, PriceType.ADJUSTED, D, 1L, 1L, 1L, 1L, 1L, 0L),
                    new StockPrice(TICKER, StockExchange.INTEGRATED, PriceType.RAW, D, 1L, 1L, 1L, 1L, 1L, 0L)));

            assertThat(repository.count()).isEqualTo(3);
        }
    }
}
