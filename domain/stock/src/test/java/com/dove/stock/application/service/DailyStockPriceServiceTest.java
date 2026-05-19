package com.dove.stock.application.service;

import com.dove.market.domain.enums.MarketType;
import com.dove.stock.domain.entity.DailyStockPrice;
import com.dove.stock.domain.entity.DailyStockPriceId;
import com.dove.stock.domain.repository.DailyStockPriceRepository;
import com.dove.stock.infrastructure.repository.DailyStockPriceRepositorySupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class DailyStockPriceServiceTest {

    @Mock DailyStockPriceRepository repository;
    @Mock DailyStockPriceRepositorySupport support;

    @InjectMocks DailyStockPriceQueryService queryService;

    private static final MarketType MARKET = MarketType.KOSPI;
    private static final LocalDate DATE = LocalDate.of(2024, 1, 3);
    private static final LocalDate DATE2 = LocalDate.of(2024, 1, 4);

    private DailyStockPrice price(String code) {
        return new DailyStockPrice(MARKET, code, DATE, 1000L, 49000L, 50000L, 48000L, 51000L);
    }

    // ─── QueryService ─────────────────────────────────────────────────────

    @Test
    @DisplayName("findRecentDailyStockPrice — 지원 레포지토리 위임")
    void shouldFindRecentDailyStockPrice() {
        List<DailyStockPrice> prices = List.of(price("005930"));
        given(support.findRecentDailyStockPrice(MARKET, "005930", DATE, 5)).willReturn(prices);

        assertThat(queryService.findRecentDailyStockPrice(MARKET, "005930", DATE, 5)).hasSize(1);
    }

    @Test
    @DisplayName("findStockCodesByMarketTypeAndTradeDate — 지원 레포지토리 위임")
    void shouldFindStockCodes() {
        given(support.findStockCodesByMarketTypeAndTradeDate(MARKET, DATE))
                .willReturn(List.of("005930", "000660"));

        assertThat(queryService.findStockCodesByMarketTypeAndTradeDate(MARKET, DATE))
                .containsExactly("005930", "000660");
    }

    @Test
    @DisplayName("existsByMarketAndCodeAndDate — JPA existsById 위임")
    void shouldCheckExistsByCodeAndDate() {
        DailyStockPriceId id = new DailyStockPriceId(MARKET, "005930", DATE);
        given(repository.existsById(id)).willReturn(true);

        assertThat(queryService.existsByMarketAndCodeAndDate(MARKET, "005930", DATE)).isTrue();
    }

    @Test
    @DisplayName("existsByMarketAndDate — 레포지토리 위임")
    void shouldCheckExistsByMarketAndDate() {
        given(repository.existsById_MarketTypeAndId_TradeDate(MARKET, DATE)).willReturn(false);

        assertThat(queryService.existsByMarketAndDate(MARKET, DATE)).isFalse();
    }

    @Test
    @DisplayName("findLatestTradeDateByMarket — 날짜 추출 반환")
    void shouldFindLatestTradeDateByMarket() {
        given(repository.findFirstById_MarketTypeOrderById_TradeDateDesc(MARKET))
                .willReturn(Optional.of(price("005930")));

        assertThat(queryService.findLatestTradeDateByMarket(MARKET)).contains(DATE);
    }

    @Test
    @DisplayName("findLatestTradeDateByMarket — 없으면 empty")
    void shouldReturnEmptyWhenNoLatestDate() {
        given(repository.findFirstById_MarketTypeOrderById_TradeDateDesc(MARKET))
                .willReturn(Optional.empty());

        assertThat(queryService.findLatestTradeDateByMarket(MARKET)).isEmpty();
    }

    @Test
    @DisplayName("findExistingTradeDatesInRange — 날짜 Set 반환")
    void shouldFindExistingDatesInRange() {
        given(support.findDistinctTradeDatesInRange(MARKET, DATE, DATE2))
                .willReturn(List.of(DATE, DATE2));

        Set<LocalDate> dates = queryService.findExistingTradeDatesInRange(MARKET, DATE, DATE2);

        assertThat(dates).containsExactlyInAnyOrder(DATE, DATE2);
    }

    @Test
    @DisplayName("findLatestTradeDate — 멀티마켓 최신 날짜 반환")
    void shouldFindLatestTradeDate() {
        List<MarketType> markets = List.of(MARKET, MarketType.KOSDAQ);
        given(support.findLatestTradeDate(markets)).willReturn(Optional.of(DATE));

        assertThat(queryService.findLatestTradeDate(markets)).contains(DATE);
    }

    @Test
    @DisplayName("findNthRecentTradeDate — 지원 레포지토리 위임")
    void shouldFindNthRecentTradeDate() {
        List<MarketType> markets = List.of(MARKET);
        given(support.findNthRecentTradeDate(markets, DATE, false, 2L))
                .willReturn(Optional.of(DATE2));

        assertThat(queryService.findNthRecentTradeDate(markets, DATE, false, 2L)).contains(DATE2);
    }

    @Test
    @DisplayName("findRecentTradeDates — 최근 N개 날짜 반환")
    void shouldFindRecentTradeDates() {
        List<MarketType> markets = List.of(MARKET);
        given(support.findRecentTradeDates(markets, DATE, 3)).willReturn(List.of(DATE, DATE2));

        assertThat(queryService.findRecentTradeDates(markets, DATE, 3)).hasSize(2);
    }

    @Test
    @DisplayName("findAllByMarketsAndDate — Map 반환")
    void shouldFindAllByMarketsAndDate() {
        List<MarketType> markets = List.of(MARKET);
        given(support.findAllByMarketsAndDate(markets, DATE))
                .willReturn(Map.of("005930", price("005930")));

        assertThat(queryService.findAllByMarketsAndDate(markets, DATE)).containsKey("005930");
    }

    // ─── CommandService ───────────────────────────────────────────────────

    @Test
    @DisplayName("save — 레포지토리 save 위임")
    void shouldSave() {
        DailyStockPriceCommandService commandService = new DailyStockPriceCommandService(repository);
        DailyStockPrice p = price("005930");
        given(repository.save(p)).willReturn(p);

        assertThat(commandService.save(p).getClosePrice()).isEqualTo(50000L);
    }
}
