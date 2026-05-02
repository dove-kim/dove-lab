package com.dove.technicalindicator.domain.service;

import com.dove.market.domain.enums.MarketType;
import com.dove.stock.application.service.StockListedDateQueryService;
import com.dove.stockprice.application.service.DailyStockPriceQueryService;
import com.dove.technicalindicator.domain.enums.DateState;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class IndicatorDateClassifierTest {

    @Mock
    private DailyStockPriceQueryService dailyStockPriceQueryService;

    @Mock
    private StockListedDateQueryService stockListedDateQueryService;

    @InjectMocks
    private IndicatorDateClassifier classifier;

    private static final MarketType MARKET = MarketType.KOSPI;
    private static final String CODE = "005930";
    private static final LocalDate DATE = LocalDate.of(2024, 1, 2);

    @Test
    @DisplayName("case A: DAILY_STOCK_PRICE 존재 → ACTIVE")
    void shouldReturnActiveWhenPriceExists() {
        given(dailyStockPriceQueryService.existsByMarketAndCodeAndDate(MARKET, CODE, DATE)).willReturn(true);

        assertThat(classifier.classify(MARKET, CODE, DATE)).isEqualTo(DateState.ACTIVE);
    }

    @Test
    @DisplayName("case B: 가격 없음, STOCK_LISTED_DATE에 종목 행 존재 → SUSPENDED")
    void shouldReturnSuspendedWhenListedButNoPriceData() {
        given(dailyStockPriceQueryService.existsByMarketAndCodeAndDate(MARKET, CODE, DATE)).willReturn(false);
        given(stockListedDateQueryService.existsByMarketAndCodeAndDate(MARKET, CODE, DATE)).willReturn(true);

        assertThat(classifier.classify(MARKET, CODE, DATE)).isEqualTo(DateState.SUSPENDED);
    }

    @Test
    @DisplayName("case C: 가격 없음, 종목 미상장, 같은 (market,date) listing ≥1건 → DELISTED")
    void shouldReturnDelistedWhenNotListedButOtherStocksAreListed() {
        given(dailyStockPriceQueryService.existsByMarketAndCodeAndDate(MARKET, CODE, DATE)).willReturn(false);
        given(stockListedDateQueryService.existsByMarketAndCodeAndDate(MARKET, CODE, DATE)).willReturn(false);
        given(stockListedDateQueryService.existsByMarketAndDate(MARKET, DATE)).willReturn(true);

        assertThat(classifier.classify(MARKET, CODE, DATE)).isEqualTo(DateState.DELISTED);
    }

    @Test
    @DisplayName("case D: 가격 없음, 종목 미상장, (market,date) listing 0건 → LISTING_NOT_SYNCED")
    void shouldReturnListingNotSyncedWhenNoListingForMarketAndDate() {
        given(dailyStockPriceQueryService.existsByMarketAndCodeAndDate(MARKET, CODE, DATE)).willReturn(false);
        given(stockListedDateQueryService.existsByMarketAndCodeAndDate(MARKET, CODE, DATE)).willReturn(false);
        given(stockListedDateQueryService.existsByMarketAndDate(MARKET, DATE)).willReturn(false);

        assertThat(classifier.classify(MARKET, CODE, DATE)).isEqualTo(DateState.LISTING_NOT_SYNCED);
    }
}
