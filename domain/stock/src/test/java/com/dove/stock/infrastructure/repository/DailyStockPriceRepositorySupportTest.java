package com.dove.stock.infrastructure.repository;

import com.dove.jpa.QuerydslConfiguration;
import com.dove.market.domain.enums.MarketType;
import com.dove.stock.domain.entity.DailyStockPrice;
import com.dove.stock.domain.repository.DailyStockPriceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import({QuerydslConfiguration.class, DailyStockPriceRepositorySupport.class})
class DailyStockPriceRepositorySupportTest {

    @Autowired
    private DailyStockPriceRepository repository;

    @Autowired
    private DailyStockPriceRepositorySupport support;

    private static final LocalDate D1 = LocalDate.of(2024, 1, 2);
    private static final LocalDate D2 = LocalDate.of(2024, 1, 3);
    private static final LocalDate D3 = LocalDate.of(2024, 1, 4);

    @BeforeEach
    void setUp() {
        repository.save(price(MarketType.KOSPI, "005930", D1, 50000L));
        repository.save(price(MarketType.KOSPI, "005930", D2, 51000L));
        repository.save(price(MarketType.KOSPI, "005930", D3, 52000L));
        repository.save(price(MarketType.KOSDAQ, "035420", D2, 80000L));
        repository.save(price(MarketType.KOSDAQ, "035420", D3, 81000L));
    }

    @Test
    @DisplayName("findRecentDailyStockPrice — 기준일 이하 최근 N건 내림차순")
    void shouldFindRecentPricesUpToDate() {
        List<DailyStockPrice> result = support.findRecentDailyStockPrice(MarketType.KOSPI, "005930", D2, 2);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getId().getTradeDate()).isEqualTo(D2);
        assertThat(result.get(1).getId().getTradeDate()).isEqualTo(D1);
    }

    @Test
    @DisplayName("findRecentDailyStockPrice — limit 초과 데이터 있어도 limit만큼만 반환")
    void shouldRespectLimit() {
        List<DailyStockPrice> result = support.findRecentDailyStockPrice(MarketType.KOSPI, "005930", D3, 2);

        assertThat(result).hasSize(2);
    }

    @Test
    @DisplayName("findStockCodesByMarketTypeAndTradeDate — 해당 날짜·시장의 종목 코드 목록")
    void shouldFindStockCodesByMarketAndDate() {
        List<String> codes = support.findStockCodesByMarketTypeAndTradeDate(MarketType.KOSPI, D2);

        assertThat(codes).containsExactly("005930");
    }

    @Test
    @DisplayName("findStockCodesByMarketTypeAndTradeDate — 데이터 없으면 빈 목록")
    void shouldReturnEmptyWhenNoDataForDate() {
        List<String> codes = support.findStockCodesByMarketTypeAndTradeDate(MarketType.KOSPI, D1.minusDays(1));

        assertThat(codes).isEmpty();
    }

    @Test
    @DisplayName("findDistinctTradeDatesInRange — 범위 내 거래일 반환")
    void shouldFindDistinctTradeDatesInRange() {
        List<LocalDate> dates = support.findDistinctTradeDatesInRange(MarketType.KOSPI, D1, D2);

        assertThat(dates).containsExactlyInAnyOrder(D1, D2);
    }

    @Test
    @DisplayName("findNthRecentTradeDate inclusive=true, offset=0 — 기준일 당일 반환")
    void shouldReturnReferenceDateWhenInclusiveAndOffsetZero() {
        Optional<LocalDate> result = support.findNthRecentTradeDate(List.of(MarketType.KOSPI), D2, true, 0);

        assertThat(result).contains(D2);
    }

    @Test
    @DisplayName("findNthRecentTradeDate inclusive=false, offset=0 — 기준일 제외 직전 거래일")
    void shouldReturnPreviousTradeDateWhenExclusive() {
        Optional<LocalDate> result = support.findNthRecentTradeDate(List.of(MarketType.KOSPI), D2, false, 0);

        assertThat(result).contains(D1);
    }

    @Test
    @DisplayName("findNthRecentTradeDate inclusive=true, offset=1 — 기준일 포함 2번째 최근 거래일")
    void shouldReturnNthDateWithOffset() {
        Optional<LocalDate> result = support.findNthRecentTradeDate(List.of(MarketType.KOSPI), D3, true, 1);

        assertThat(result).contains(D2);
    }

    @Test
    @DisplayName("findNthRecentTradeDate — 데이터 없으면 empty")
    void shouldReturnEmptyWhenNoData() {
        Optional<LocalDate> result = support.findNthRecentTradeDate(List.of(MarketType.KOSPI), D1.minusDays(1), true, 0);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("findLatestTradeDate — 여러 시장 중 가장 최신 거래일 반환")
    void shouldFindLatestTradeDateAcrossMarkets() {
        Optional<LocalDate> result = support.findLatestTradeDate(List.of(MarketType.KOSPI, MarketType.KOSDAQ));

        assertThat(result).contains(D3);
    }

    @Test
    @DisplayName("findLatestTradeDate — 데이터 없으면 empty")
    void shouldReturnEmptyForLatestTradeDateWhenNoData() {
        Optional<LocalDate> result = support.findLatestTradeDate(List.of(MarketType.KONEX));

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("findRecentTradeDates — to 이하 최근 N개 거래일 내림차순")
    void shouldFindRecentTradeDates() {
        List<LocalDate> dates = support.findRecentTradeDates(List.of(MarketType.KOSPI), D2, 2);

        assertThat(dates).containsExactly(D2, D1);
    }

    @Test
    @DisplayName("findAllByMarketsAndDate — 특정 날짜 시장별 주가 맵 반환")
    void shouldFindAllPricesByMarketsAndDate() {
        Map<String, DailyStockPrice> result = support.findAllByMarketsAndDate(
                List.of(MarketType.KOSPI, MarketType.KOSDAQ), D3);

        assertThat(result).hasSize(2);
        assertThat(result).containsKey("005930");
        assertThat(result).containsKey("035420");
        assertThat(result.get("005930").getClosePrice()).isEqualTo(52000L);
    }

    @Test
    @DisplayName("findAllByMarketsAndDate — 해당 날짜 데이터 없으면 빈 맵")
    void shouldReturnEmptyMapWhenNoDataForDate() {
        Map<String, DailyStockPrice> result = support.findAllByMarketsAndDate(
                List.of(MarketType.KOSPI), D1.minusDays(1));

        assertThat(result).isEmpty();
    }

    private DailyStockPrice price(MarketType market, String code, LocalDate date, Long closePrice) {
        return new DailyStockPrice(market, code, date, 1000L, closePrice - 500L, closePrice, closePrice - 1000L, closePrice + 500L);
    }
}
