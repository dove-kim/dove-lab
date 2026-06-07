package com.dove.stockcollection.application.service;

import com.dove.indicator.application.service.IndicatorCursorService;
import com.dove.stock.application.service.StockPriceCommandService;
import com.dove.stock.application.service.StockQueryService;
import com.dove.stock.domain.entity.StockPrice;
import com.dove.stock.domain.enums.PriceType;
import com.dove.stock.domain.enums.StockExchange;
import com.dove.stockcollection.application.port.DailyPriceFetcher;
import com.dove.stockcollection.domain.model.DailyCandle;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.List;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willAnswer;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;

/**
 * PriceCollectionService 의 주가 수집·저장과 지표 커서 조정 행위를 검증한다.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PriceCollectionService")
class PriceCollectionServiceTest {

    private static final StockExchange EXCHANGE = StockExchange.KOSPI;
    private static final LocalDate FROM = LocalDate.of(2024, 1, 1);
    private static final LocalDate TO = LocalDate.of(2024, 12, 31);
    private static final LocalDate ADJUSTED_FROM = LocalDate.of(1985, 1, 1);

    @Mock
    private DailyPriceFetcher fetcher;
    @Mock
    private StockQueryService stockQueryService;
    @Mock
    private StockPriceCommandService priceCommandService;
    @Mock
    private IndicatorCursorService cursorService;

    @InjectMocks
    private PriceCollectionService service;

    @BeforeEach
    void setUp() {
        // @Value concurrency가 Spring 없이는 0 → Parallel의 Semaphore(0)로 영구 블로킹되므로 직접 주입
        ReflectionTestUtils.setField(service, "concurrency", 4);
    }

    /**
     * RAW 가격유형 단일 캔들 청크를 consumer에 전달하도록 fetchInWindows를 스텁한다.
     */
    private void stubFetchInWindows(List<DailyCandle> chunk) {
        willAnswer(invocation -> {
            PriceType priceType = invocation.getArgument(4);
            Consumer<List<DailyCandle>> consumer = invocation.getArgument(5);
            // RAW만 수정주가 이벤트 감지 대상 — 두 가격유형 모두 동일 청크를 흘려보낸다
            consumer.accept(chunk);
            return null;
        }).given(fetcher).fetchInWindows(eq(EXCHANGE), any(), any(), any(), any(), any());
    }

    private DailyCandle candle(LocalDate date, String adjustmentCode) {
        return new DailyCandle(date, 1000L, 1100L, 900L, 1050L, 5000L, 5_250_000L, adjustmentCode);
    }

    @Nested
    @DisplayName("collect: 대상 종목 없음")
    class NoTickers {

        @Test
        @DisplayName("종목이 없으면 onTotal(0)만 호출하고 수집·저장을 하지 않는다")
        void shouldReportZeroTotalAndSkipFetchWhenNoTickers() {
            given(stockQueryService.findTickersByExchange(EXCHANGE)).willReturn(List.of());
            CollectionProgress progress = org.mockito.Mockito.mock(CollectionProgress.class);

            service.collect(EXCHANGE, FROM, TO, progress, ADJUSTED_FROM);

            then(progress).should().onTotal(0);
            then(fetcher).shouldHaveNoInteractions();
            then(priceCommandService).shouldHaveNoInteractions();
            then(cursorService).shouldHaveNoInteractions();
        }
    }

    @Nested
    @DisplayName("collect: 정상 수집")
    class NormalCollection {

        @Test
        @DisplayName("종목×가격유형 단위로 수집하고 청크를 upsertAll로 저장한 뒤 거래소 커서를 from 직전으로 되돌린다")
        void shouldFetchPerUnitStoreChunkAndRewindCursorWhenCollected() {
            given(stockQueryService.findTickersByExchange(EXCHANGE)).willReturn(List.of("005930"));
            // 이벤트 코드 "00"(해당없음) → 수정주가 재조회 트리거 안 함
            stubFetchInWindows(List.of(candle(LocalDate.of(2024, 3, 4), "00")));
            CollectionProgress progress = org.mockito.Mockito.mock(CollectionProgress.class);

            service.collect(EXCHANGE, FROM, TO, progress, ADJUSTED_FROM);

            // 1종목 × 2가격유형(RAW, ADJUSTED) = 2작업
            then(progress).should().onTotal(2);
            then(fetcher).should().fetchInWindows(eq(EXCHANGE), eq("005930"), eq(FROM), eq(TO), eq(PriceType.RAW), any());
            then(fetcher).should().fetchInWindows(eq(EXCHANGE), eq("005930"), eq(FROM), eq(TO), eq(PriceType.ADJUSTED), any());

            ArgumentCaptor<List<StockPrice>> captor = ArgumentCaptor.forClass(List.class);
            then(priceCommandService).should(atLeastOnce()).upsertAll(captor.capture());
            StockPrice saved = captor.getValue().get(0);
            assertThat(saved.getTicker()).isEqualTo("005930");
            assertThat(saved.getExchange()).isEqualTo(EXCHANGE);
            assertThat(saved.getTradeDate()).isEqualTo(LocalDate.of(2024, 3, 4));
            assertThat(saved.getClosePrice()).isEqualTo(1050L);

            then(cursorService).should().rewindExchangeBefore(EXCHANGE, FROM);
            // 수정주가 이벤트 없음 → 역방향 재조회·커서 삭제 없음
            then(fetcher).should(never()).fetchAdjustedBackward(any(), any(), any(), any(), any());
            then(cursorService).should(never()).clearAdjusted(any(), any());
        }
    }

    @Nested
    @DisplayName("collect: 수정주가 이벤트 감지")
    class AdjustmentEvent {

        @Test
        @DisplayName("RAW 캔들에 수정주가 이벤트가 있고 adjustedFrom이 있으면 ADJUSTED 역방향 재조회와 커서 삭제를 한다")
        void shouldRefetchAdjustedAndClearCursorWhenAdjustmentEventAndAdjustedFromPresent() {
            given(stockQueryService.findTickersByExchange(EXCHANGE)).willReturn(List.of("005930"));
            // "02"(배당락) → 수정주가 이벤트
            stubFetchInWindows(List.of(candle(LocalDate.of(2024, 3, 4), "02")));
            // 역방향 재조회 스텁 — 청크를 consumer에 흘려보내 저장 흐름 재현
            willAnswer(invocation -> {
                Consumer<List<DailyCandle>> consumer = invocation.getArgument(4);
                consumer.accept(List.of(candle(LocalDate.of(2024, 3, 4), "00")));
                return null;
            }).given(fetcher).fetchAdjustedBackward(eq(EXCHANGE), eq("005930"), eq(ADJUSTED_FROM), eq(TO), any());

            service.collect(EXCHANGE, FROM, TO, CollectionProgress.NOOP, ADJUSTED_FROM);

            then(cursorService).should().rewindExchangeBefore(EXCHANGE, FROM);
            then(fetcher).should().fetchAdjustedBackward(eq(EXCHANGE), eq("005930"), eq(ADJUSTED_FROM), eq(TO), any());
            then(cursorService).should().clearAdjusted("005930", EXCHANGE);
        }

        @Test
        @DisplayName("adjustedFrom이 null이면 이벤트가 있어도 재조회·커서 삭제를 하지 않는다")
        void shouldNotRefetchWhenAdjustedFromNull() {
            given(stockQueryService.findTickersByExchange(EXCHANGE)).willReturn(List.of("005930"));
            stubFetchInWindows(List.of(candle(LocalDate.of(2024, 3, 4), "02")));

            service.collect(EXCHANGE, FROM, TO, CollectionProgress.NOOP, null);

            // from 직전 커서 되돌림은 여전히 수행
            then(cursorService).should().rewindExchangeBefore(EXCHANGE, FROM);
            // 기록만 — 역방향 재조회·ADJUSTED 커서 삭제는 생략
            then(fetcher).should(never()).fetchAdjustedBackward(any(), any(), any(), any(), any());
            then(cursorService).should(never()).clearAdjusted(any(), any());
            // 정상 수집 저장은 발생
            then(priceCommandService).should(atLeastOnce()).upsertAll(anyList());
        }
    }
}
