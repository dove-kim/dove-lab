package com.dove.scheduler.service;

import com.dove.krx.StockListing;
import com.dove.krx.TradingDayPort;
import com.dove.market.domain.enums.MarketType;
import com.dove.stock.application.service.StockCommandService;
import com.dove.stock.application.service.StockTagValueService;
import com.dove.stock.domain.entity.Stock;
import com.dove.stock.domain.enums.TagField;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * StockSyncService 단위 테스트.
 */
@ExtendWith(MockitoExtension.class)
class StockSyncServiceTest {

    @Mock TradingDayPort tradingDayPort;
    @Mock StockCommandService stockCommandService;
    @Mock StockTagValueService tagValueCommandService;
    @InjectMocks StockSyncService service;

    private static final MarketType MARKET = MarketType.KOSPI;

    private StockListing listing(String ticker, LocalDate listingDate, String secugrp, String kind) {
        return new StockListing(ticker, "ISIN-" + ticker, listingDate, secugrp, kind);
    }

    @Nested
    @DisplayName("syncRange — 중복 제거 후 insert")
    class SyncRange {

        @Test
        @DisplayName("같은 ticker가 여러 날짜에 나오면 최신 날짜로 덮어써 1건만 insert한다")
        void shouldKeepLatestDateWhenSameTickerAppearsOnMultipleDates() {
            LocalDate from = LocalDate.of(2026, 6, 1);
            LocalDate to = LocalDate.of(2026, 6, 2);
            given(tradingDayPort.fetchListings(MARKET, from))
                    .willReturn(List.of(listing("005930", from, "주권", "보통주")));
            given(tradingDayPort.fetchListings(MARKET, to))
                    .willReturn(List.of(listing("005930", to, "주권", "보통주")));

            service.syncRange(MARKET, from, to, () -> {});

            ArgumentCaptor<List<Stock>> captor = ArgumentCaptor.forClass(List.class);
            verify(stockCommandService).insertIfAbsent(captor.capture());
            List<Stock> inserted = captor.getValue();
            assertThat(inserted).hasSize(1);
            assertThat(inserted.get(0).getTicker()).isEqualTo("005930");
            assertThat(inserted.get(0).getListingDate()).isEqualTo(to);
        }

        @Test
        @DisplayName("날짜마다 onDateFetched를 날짜 수만큼 호출한다")
        void shouldInvokeOnDateFetchedPerDate() {
            LocalDate from = LocalDate.of(2026, 6, 1);
            LocalDate to = LocalDate.of(2026, 6, 3);
            given(tradingDayPort.fetchListings(eq(MARKET), org.mockito.ArgumentMatchers.any()))
                    .willReturn(List.of(listing("005930", from, "주권", "보통주")));

            AtomicInteger count = new AtomicInteger();
            service.syncRange(MARKET, from, to, count::incrementAndGet);

            assertThat(count.get()).isEqualTo(3);
        }

        @Test
        @DisplayName("수집 결과가 없으면 insertIfAbsent를 호출하지 않는다")
        void shouldNotInsertWhenNoListingsCollected() {
            LocalDate date = LocalDate.of(2026, 6, 1);
            given(tradingDayPort.fetchListings(MARKET, date)).willReturn(List.of());

            service.syncRange(MARKET, date, date, () -> {});

            verify(stockCommandService, never()).insertIfAbsent(org.mockito.ArgumentMatchers.anyList());
            verify(tagValueCommandService, never())
                    .registerIfAbsent(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyString());
        }

        @Test
        @DisplayName("증권그룹·주권종류를 distinct로 태그 마스터에 등록한다")
        void shouldRegisterDistinctSecugrpAndKindTags() {
            LocalDate date = LocalDate.of(2026, 6, 1);
            given(tradingDayPort.fetchListings(MARKET, date)).willReturn(List.of(
                    listing("005930", date, "주권", "보통주"),
                    listing("000660", date, "주권", "보통주"),
                    listing("068270", date, "주권", "우선주")));

            service.syncRange(MARKET, date, date, () -> {});

            verify(tagValueCommandService).registerIfAbsent(TagField.SECUGRP.name(), "주권");
            verify(tagValueCommandService).registerIfAbsent(TagField.STOCK_TYPE.name(), "보통주");
            verify(tagValueCommandService).registerIfAbsent(TagField.STOCK_TYPE.name(), "우선주");
        }
    }
}
