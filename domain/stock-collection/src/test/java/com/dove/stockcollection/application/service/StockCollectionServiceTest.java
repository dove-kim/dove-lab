package com.dove.stockcollection.application.service;

import com.dove.stockcollection.application.port.StockListing;
import com.dove.stockcollection.application.port.TradingDayPort;
import com.dove.market.domain.enums.MarketType;
import com.dove.stock.application.service.StockCommandService;
import com.dove.stock.application.service.StockTagValueService;
import com.dove.stock.domain.entity.Stock;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("StockCollectionService")
class StockCollectionServiceTest {

    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");

    // 고정 시계: 오늘 = 2026-05-31 (KST)
    private final Clock clock = Clock.fixed(Instant.parse("2026-05-31T00:00:00Z"), SEOUL);

    @Mock
    private TradingDayPort tradingDayPort;

    @Mock
    private StockCommandService stockCommandService;

    @Mock
    private StockTagValueService tagValueService;

    private StockCollectionService createService() {
        StockCollectionService svc =
                new StockCollectionService(tradingDayPort, stockCommandService, tagValueService, clock);
        ReflectionTestUtils.setField(svc, "concurrency", 4);
        return svc;
    }

    private StockListing listing(String ticker) {
        return new StockListing(ticker, "KR" + ticker, LocalDate.of(2000, 1, 1), "주권", "보통주");
    }

    @Nested
    @DisplayName("collect")
    class Collect {

        @Test
        @DisplayName("from이 오늘 이후면 total 0을 통보하고 수집하지 않는다")
        void shouldSkipWhenFromIsAfterToday() {
            StockCollectionService service = createService();
            CollectionProgress progress = mock();

            service.collect(LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 5), progress);

            verify(progress).onTotal(0);
            verify(progress, never()).onProgress(org.mockito.ArgumentMatchers.anyInt());
            verifyNoInteractions(tradingDayPort, stockCommandService);
        }

        @Test
        @DisplayName("정상 기간이면 날짜×시장별 fetchListings 후 insertIfAbsent를 1회 호출한다")
        void shouldFetchAndInsertPerDateAndMarketWhenRangeIsValid() {
            StockCollectionService service = createService();
            CollectionProgress progress = mock();

            // 2일 × 3시장 = 6회 fetch. 모든 호출에 동일 ticker 1건씩 반환.
            when(tradingDayPort.fetchListings(any(MarketType.class), any(LocalDate.class)))
                    .thenReturn(List.of(listing("005930")));

            LocalDate from = LocalDate.of(2026, 5, 29);
            LocalDate to = LocalDate.of(2026, 5, 30);

            service.collect(from, to, progress);

            // 날짜×시장 평탄화: total = 2일 × 3시장 = 6
            verify(progress).onTotal(6);
            verify(tradingDayPort, times(6))
                    .fetchListings(any(MarketType.class), any(LocalDate.class));
            for (MarketType market : MarketType.KRX_MARKETS) {
                verify(tradingDayPort).fetchListings(market, from);
                verify(tradingDayPort).fetchListings(market, to);
            }
            // 병렬 fetch 완료 후 단일 insert — ticker 중복 제거로 1건
            ArgumentCaptor<List<Stock>> captor = ArgumentCaptor.forClass(List.class);
            verify(stockCommandService, times(1)).insertIfAbsent(captor.capture());
            assertThat(captor.getValue()).hasSize(1);
        }

        @Test
        @DisplayName("어떤 날짜의 목록이 비어 있으면 그 호출은 insertIfAbsent를 건너뛴다")
        void shouldSkipInsertWhenListingsEmpty() {
            StockCollectionService service = createService();

            // 모든 시장/날짜에서 빈 목록
            when(tradingDayPort.fetchListings(any(MarketType.class), any(LocalDate.class)))
                    .thenReturn(List.of());

            service.collect(LocalDate.of(2026, 5, 30), LocalDate.of(2026, 5, 30), CollectionProgress.NOOP);

            verify(tradingDayPort, times(3))
                    .fetchListings(any(MarketType.class), any(LocalDate.class));
            verify(stockCommandService, never()).insertIfAbsent(any());
        }

        @Test
        @DisplayName("to가 오늘 이후면 오늘로 캡한다")
        void shouldCapToToTodayWhenToIsAfterToday() {
            StockCollectionService service = createService();
            CollectionProgress progress = mock();

            when(tradingDayPort.fetchListings(any(MarketType.class), any(LocalDate.class)))
                    .thenReturn(List.of());

            LocalDate from = LocalDate.of(2026, 5, 30);
            LocalDate futureTo = LocalDate.of(2026, 6, 10);

            service.collect(from, futureTo, progress);

            // 5/30~5/31(오늘) = 2일 × 3시장 = 6건으로 캡
            verify(progress).onTotal(6);
            LocalDate today = LocalDate.of(2026, 5, 31);
            for (MarketType market : MarketType.KRX_MARKETS) {
                verify(tradingDayPort).fetchListings(market, today);
                verify(tradingDayPort).fetchListings(market, from);
            }
            // 미래일은 절대 조회하지 않음
            verify(tradingDayPort, never()).fetchListings(any(MarketType.class), eq(futureTo));
        }
    }
}
