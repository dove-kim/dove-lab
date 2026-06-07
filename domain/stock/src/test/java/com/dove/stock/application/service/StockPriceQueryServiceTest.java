package com.dove.stock.application.service;

import com.dove.jpa.QuerydslConfiguration;
import com.dove.market.domain.enums.MarketType;
import com.dove.stock.domain.entity.StockPrice;
import com.dove.stock.domain.enums.PriceType;
import com.dove.stock.domain.enums.StockExchange;
import com.dove.stock.domain.repository.StockPriceRepository;
import com.dove.stock.infrastructure.repository.StockPriceRepositorySupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * StockPriceQueryService 통합 테스트.
 */
@DataJpaTest
@Import({StockPriceQueryService.class, StockPriceRepositorySupport.class, QuerydslConfiguration.class})
class StockPriceQueryServiceTest {

    private static final String TICKER = "005930";
    private static final StockExchange EX = StockExchange.KOSPI;
    private static final PriceType PT = PriceType.RAW;

    @Autowired StockPriceQueryService service;
    @Autowired StockPriceRepository repository;

    private void save(LocalDate date) {
        repository.save(new StockPrice(TICKER, EX, PT, date, 100L, 110L, 90L, 100L, 1000L, 0L));
    }

    private void save(String ticker, StockExchange exchange, LocalDate date) {
        repository.save(new StockPrice(ticker, exchange, PT, date, 100L, 110L, 90L, 100L, 1000L, 0L));
    }

    @Nested
    @DisplayName("findChunk — 구간 조회")
    class FindChunk {

        @Test
        @DisplayName("구간 내 행을 거래일 오름차순으로 limit 만큼 반환한다")
        void shouldReturnAscendingUpToLimitWhenFindChunk() {
            save(LocalDate.of(2024, 1, 3));
            save(LocalDate.of(2024, 1, 1));
            save(LocalDate.of(2024, 1, 4));
            save(LocalDate.of(2024, 1, 2));

            List<StockPrice> result = service.findChunk(
                    TICKER, EX, PT, LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 4), 2);

            assertThat(result).extracting(StockPrice::getTradeDate)
                    .containsExactly(LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 2));
        }

        @Test
        @DisplayName("fromInclusive 이전 행은 포함하지 않는다")
        void shouldExcludeBeforeFromWhenFindChunk() {
            save(LocalDate.of(2024, 1, 1));
            save(LocalDate.of(2024, 1, 2));
            save(LocalDate.of(2024, 1, 3));

            List<StockPrice> result = service.findChunk(
                    TICKER, EX, PT, LocalDate.of(2024, 1, 2), LocalDate.of(2024, 1, 3), 100);

            assertThat(result).extracting(StockPrice::getTradeDate)
                    .containsExactly(LocalDate.of(2024, 1, 2), LocalDate.of(2024, 1, 3));
        }
    }

    @Nested
    @DisplayName("findBefore — lookback 조회")
    class FindBefore {

        @Test
        @DisplayName("beforeExclusive 직전 N개를 오름차순으로 반환한다")
        void shouldReturnLookbackAscendingWhenFindBefore() {
            save(LocalDate.of(2024, 1, 1));
            save(LocalDate.of(2024, 1, 2));
            save(LocalDate.of(2024, 1, 3));
            save(LocalDate.of(2024, 1, 4));

            List<StockPrice> result = service.findBefore(TICKER, EX, PT, LocalDate.of(2024, 1, 4), 2);

            assertThat(result).extracting(StockPrice::getTradeDate)
                    .containsExactly(LocalDate.of(2024, 1, 2), LocalDate.of(2024, 1, 3));
        }
    }

    @Nested
    @DisplayName("countBefore — 직전 행 수")
    class CountBefore {

        @Test
        @DisplayName("beforeExclusive 직전 행 수를 반환한다")
        void shouldReturnCountBeforeDate() {
            save(LocalDate.of(2024, 1, 1));
            save(LocalDate.of(2024, 1, 2));
            save(LocalDate.of(2024, 1, 3));

            assertThat(service.countBefore(TICKER, EX, PT, LocalDate.of(2024, 1, 3))).isEqualTo(2);
        }

        @Test
        @DisplayName("직전 행이 없으면 0을 반환한다")
        void shouldReturnZeroWhenNoBefore() {
            save(LocalDate.of(2024, 1, 1));

            assertThat(service.countBefore(TICKER, EX, PT, LocalDate.of(2024, 1, 1))).isZero();
        }
    }

    @Nested
    @DisplayName("findRecent — 최근 N 거래일")
    class FindRecent {

        @Test
        @DisplayName("최근 N 거래일을 오름차순으로 반환한다")
        void shouldReturnRecentAscendingWhenFindRecent() {
            save(LocalDate.of(2024, 1, 1));
            save(LocalDate.of(2024, 1, 2));
            save(LocalDate.of(2024, 1, 3));
            save(LocalDate.of(2024, 1, 4));
            save(LocalDate.of(2024, 1, 5));

            List<StockPrice> result = service.findRecent(TICKER, MarketType.KOSPI, PT, 3);

            assertThat(result).extracting(StockPrice::getTradeDate)
                    .containsExactly(LocalDate.of(2024, 1, 3), LocalDate.of(2024, 1, 4), LocalDate.of(2024, 1, 5));
        }
    }

    @Nested
    @DisplayName("findByMarketsAndDate — 날짜·시장별 전 종목 주가")
    class FindByMarketsAndDate {

        @Test
        @DisplayName("지정 시장·날짜에 해당하는 종목 주가를 ticker 키 맵으로 반환한다")
        void shouldReturnPricesByMarketAndDate() {
            save("005930", StockExchange.KOSPI, LocalDate.of(2024, 1, 2));
            save("000660", StockExchange.KOSDAQ, LocalDate.of(2024, 1, 2));
            save("005930", StockExchange.KOSPI, LocalDate.of(2024, 1, 3));

            Map<String, StockPrice> result = service.findByMarketsAndDate(
                    List.of(MarketType.KOSPI), PT, LocalDate.of(2024, 1, 2));

            assertThat(result).containsOnlyKeys("005930");
        }
    }

    @Nested
    @DisplayName("findTradeDatesInRange — 구간 거래일")
    class FindTradeDatesInRange {

        @Test
        @DisplayName("구간 내 거래일을 오름차순으로 반환한다")
        void shouldReturnTradeDatesAscendingInRange() {
            save(LocalDate.of(2024, 1, 1));
            save(LocalDate.of(2024, 1, 3));
            save(LocalDate.of(2024, 1, 5));

            List<LocalDate> result = service.findTradeDatesInRange(
                    List.of(MarketType.KOSPI), PT,
                    LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 4));

            assertThat(result).containsExactly(LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 3));
        }
    }

    @Nested
    @DisplayName("findRecentTradeDates — 최근 거래일 목록")
    class FindRecentTradeDates {

        @Test
        @DisplayName("onOrBefore 이하 최근 N개 거래일을 내림차순으로 반환한다")
        void shouldReturnRecentTradeDatesDescending() {
            save(LocalDate.of(2024, 1, 1));
            save(LocalDate.of(2024, 1, 2));
            save(LocalDate.of(2024, 1, 3));

            List<LocalDate> result = service.findRecentTradeDates(
                    List.of(MarketType.KOSPI), PT, LocalDate.of(2024, 1, 3), 2);

            assertThat(result).containsExactly(LocalDate.of(2024, 1, 3), LocalDate.of(2024, 1, 2));
        }
    }

    @Nested
    @DisplayName("findNthRecentTradeDate — N번째 최근 거래일")
    class FindNthRecentTradeDate {

        @Test
        @DisplayName("offset 0은 가장 최근 거래일을 반환한다")
        void shouldReturnMostRecentWhenOffsetZero() {
            save(LocalDate.of(2024, 1, 1));
            save(LocalDate.of(2024, 1, 2));
            save(LocalDate.of(2024, 1, 3));

            LocalDate result = service.findNthRecentTradeDate(
                    List.of(MarketType.KOSPI), PT, LocalDate.of(2024, 1, 3), 0);

            assertThat(result).isEqualTo(LocalDate.of(2024, 1, 3));
        }

        @Test
        @DisplayName("데이터가 없으면 null을 반환한다")
        void shouldReturnNullWhenNoData() {
            LocalDate result = service.findNthRecentTradeDate(
                    List.of(MarketType.KOSPI), PT, LocalDate.of(2024, 1, 3), 0);

            assertThat(result).isNull();
        }
    }
}
