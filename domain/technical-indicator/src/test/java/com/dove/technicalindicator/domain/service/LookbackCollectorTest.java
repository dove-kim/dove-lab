package com.dove.technicalindicator.domain.service;

import com.dove.market.application.service.MarketTradingDateQueryService;
import com.dove.market.domain.enums.MarketType;
import com.dove.stockprice.application.service.DailyStockPriceQueryService;
import com.dove.stockprice.domain.entity.DailyStockPrice;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class LookbackCollectorTest {

    @Mock
    private DailyStockPriceQueryService priceQueryService;
    @Mock
    private MarketTradingDateQueryService marketTradingDateQueryService;

    @InjectMocks
    private LookbackCollector collector;

    private static final MarketType MARKET = MarketType.KOSPI;
    private static final String CODE = "005930";

    private DailyStockPrice price(int year, int month, int day) {
        LocalDate date = LocalDate.of(year, month, day);
        return new DailyStockPrice(MARKET, CODE, date, 1000L, 1000L, 1000L, 1000L, 1000L);
    }

    @Test
    @DisplayName("연속 N일 데이터가 있으면 pool 크기가 N이고 날짜 오름차순이다")
    void shouldReturnNRowsInAscendingOrder() {
        LocalDate nextDay = LocalDate.of(2024, 1, 5);
        given(priceQueryService.findRecentDailyStockPrice(MARKET, CODE, nextDay, 3))
                .willReturn(List.of(price(2024, 1, 5), price(2024, 1, 4), price(2024, 1, 3)));

        List<DailyStockPrice> pool = collector.collect(MARKET, CODE, nextDay, 3);

        assertThat(pool).hasSize(3);
        assertThat(pool.get(0).getId().getTradeDate()).isEqualTo(LocalDate.of(2024, 1, 3));
        assertThat(pool.get(2).getId().getTradeDate()).isEqualTo(LocalDate.of(2024, 1, 5));
    }

    @Test
    @DisplayName("갭 구간에 시장이 열린 날이 있으면 수집 중단 — 정지·폐지·미동기화 무관하게 단절")
    void shouldStopCollectionWhenMarketWasOpenDuringGap() {
        LocalDate nextDay = LocalDate.of(2024, 1, 5);
        given(priceQueryService.findRecentDailyStockPrice(MARKET, CODE, nextDay, 3))
                .willReturn(List.of(price(2024, 1, 5), price(2024, 1, 3)));
        // 2024-01-04: 시장은 열렸지만 이 종목 주가 없음 → 연속성 단절
        given(marketTradingDateQueryService.existsOpenDay(MARKET, LocalDate.of(2024, 1, 4))).willReturn(true);

        List<DailyStockPrice> pool = collector.collect(MARKET, CODE, nextDay, 3);

        assertThat(pool).hasSize(1);
        assertThat(pool.get(0).getId().getTradeDate()).isEqualTo(LocalDate.of(2024, 1, 5));
    }

    @Test
    @DisplayName("주말·휴장 갭 — MarketTradingDate 없으면 단절 없이 스킵")
    void shouldSkipGapsWithNoMarketData() {
        // 2024-01-08(월), 2024-01-05(금) — 토/일 갭 (existsOpenDay=false → 스킵)
        LocalDate nextDay = LocalDate.of(2024, 1, 8);
        given(priceQueryService.findRecentDailyStockPrice(MARKET, CODE, nextDay, 2))
                .willReturn(List.of(price(2024, 1, 8), price(2024, 1, 5)));
        given(marketTradingDateQueryService.existsOpenDay(MARKET, LocalDate.of(2024, 1, 7))).willReturn(false);
        given(marketTradingDateQueryService.existsOpenDay(MARKET, LocalDate.of(2024, 1, 6))).willReturn(false);

        List<DailyStockPrice> pool = collector.collect(MARKET, CODE, nextDay, 2);

        assertThat(pool).hasSize(2);
    }

    @Test
    @DisplayName("데이터가 size 미만이면 있는 것만 반환한다")
    void shouldReturnFewerItemsWhenNotEnoughData() {
        LocalDate nextDay = LocalDate.of(2024, 1, 5);
        given(priceQueryService.findRecentDailyStockPrice(MARKET, CODE, nextDay, 5))
                .willReturn(List.of(price(2024, 1, 5)));

        List<DailyStockPrice> pool = collector.collect(MARKET, CODE, nextDay, 5);

        assertThat(pool).hasSize(1);
    }
}
