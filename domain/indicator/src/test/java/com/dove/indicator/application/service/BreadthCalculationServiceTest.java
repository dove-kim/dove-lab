package com.dove.indicator.application.service;

import com.dove.indicator.application.exception.BreadthCursorRewoundException;
import com.dove.indicator.domain.breadth.entity.BreadthCursor;
import com.dove.indicator.domain.breadth.entity.StockBreadthDaily;
import com.dove.indicator.domain.entity.StockFeatureDaily;
import com.dove.indicator.domain.entity.StockFeatureDailyId;
import com.dove.indicator.domain.enums.IndicatorType;
import com.dove.indicator.infrastructure.repository.RankSourceRepositorySupport;
import com.dove.indicator.infrastructure.repository.StockFeatureDailyRepositorySupport;
import com.dove.stock.domain.enums.MarketUniverse;
import com.dove.stock.domain.enums.PriceType;
import com.dove.stock.domain.enums.StockExchange;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BreadthCalculationServiceTest {

    private static final MarketUniverse UNIV = MarketUniverse.KRX;
    private static final List<StockExchange> MEMBERS = UNIV.members();
    private static final PriceType PT = PriceType.RAW;
    private static final LocalDate D1 = LocalDate.of(2024, 1, 1);

    private BreadthCursorService cursorService;
    private RankSourceRepositorySupport sourceSupport;
    private StockFeatureDailyRepositorySupport featureSupport;
    private BreadthDateCommitService commitService;
    private BreadthCalculationService service;

    @BeforeEach
    void setUp() {
        cursorService = mock(BreadthCursorService.class);
        sourceSupport = mock(RankSourceRepositorySupport.class);
        featureSupport = mock(StockFeatureDailyRepositorySupport.class);
        commitService = mock(BreadthDateCommitService.class);
        service = new BreadthCalculationService(cursorService, sourceSupport, featureSupport, commitService);
    }

    /** RET_1D 값만 채운 한 종목의 피처 행. */
    private StockFeatureDaily feature(String ticker, StockExchange exchange, LocalDate date, Double ret1d) {
        StockFeatureDailyId id = new StockFeatureDailyId(ticker, exchange, PT, date);
        StockFeatureDaily row = new StockFeatureDaily(id, 1, 100L, 110L, 90L, 100L, 1000L, 100L,
                LocalDateTime.now());
        if (ret1d != null) row.set(IndicatorType.RET_1D, ret1d);
        return row;
    }

    private void stubCursor(LocalDate cursorDate, boolean exists) {
        Optional<BreadthCursor> opt;
        if (!exists) {
            opt = Optional.empty();
        } else {
            BreadthCursor c = new BreadthCursor(UNIV, PT);
            if (cursorDate != null) c.advance(cursorDate);
            opt = Optional.of(c);
        }
        when(cursorService.findCursor(UNIV, PT)).thenReturn(opt);
    }

    @SuppressWarnings("unchecked")
    private List<StockBreadthDaily> committedRowsFor(LocalDate date) {
        ArgumentCaptor<List<StockBreadthDaily>> captor = ArgumentCaptor.forClass(List.class);
        verify(commitService, atLeastOnce())
                .commit(any(), any(), captor.capture(), any(), anyBoolean(), any());
        return captor.getAllValues().stream().flatMap(List::stream)
                .filter(r -> r.getId().getTradeDate().equals(date))
                .collect(Collectors.toList());
    }

    @Nested
    @DisplayName("calculateUniverse")
    class CalculateUniverse {

        @Test
        @DisplayName("지표 프런티어가 없으면(미완비) 아무 것도 계산·저장하지 않는다")
        void shouldSkipWhenNoFrontier() {
            stubCursor(null, false);
            when(sourceSupport.findIndicatorFrontier(MEMBERS, PT)).thenReturn(null);

            service.calculateUniverse(UNIV, PT);

            verify(commitService, never()).commit(any(), any(), any(), any(), anyBoolean(), any());
        }

        @Test
        @DisplayName("프런티어까지 각 거래일별로 상승비율을 계산하고 그 날짜로 커서를 전진한다")
        void shouldComputePerDateAndAdvanceCursor() {
            stubCursor(null, false);
            when(sourceSupport.findIndicatorFrontier(MEMBERS, PT)).thenReturn(D1.plusDays(1));
            when(sourceSupport.findFeatureTradeDates(eq(MEMBERS), eq(PT), eq(null), eq(D1.plusDays(1))))
                    .thenReturn(List.of(D1, D1.plusDays(1)));
            when(featureSupport.findByExchangesAndPriceTypeAndDate(MEMBERS, PT, D1))
                    .thenReturn(List.of(
                            feature("A", StockExchange.KOSPI, D1, 10.0),
                            feature("B", StockExchange.KOSPI, D1, -5.0)));
            when(featureSupport.findByExchangesAndPriceTypeAndDate(MEMBERS, PT, D1.plusDays(1)))
                    .thenReturn(List.of(feature("A", StockExchange.KOSPI, D1.plusDays(1), 5.0)));

            service.calculateUniverse(UNIV, PT);

            ArgumentCaptor<LocalDate> commitDate = ArgumentCaptor.forClass(LocalDate.class);
            verify(commitService, times(2))
                    .commit(eq(UNIV), eq(PT), any(), any(), anyBoolean(), commitDate.capture());
            assertThat(commitDate.getAllValues()).containsExactly(D1, D1.plusDays(1));
        }

        @Test
        @DisplayName("KOSPI·KOSDAQ를 풀링해 단일 상승비율을 각 member 거래소 행으로 중복 저장한다")
        void shouldPoolMembersAndReplicateRatioPerMember() {
            stubCursor(null, false);
            when(sourceSupport.findIndicatorFrontier(MEMBERS, PT)).thenReturn(D1);
            when(sourceSupport.findFeatureTradeDates(eq(MEMBERS), eq(PT), eq(null), eq(D1)))
                    .thenReturn(List.of(D1));
            when(featureSupport.findByExchangesAndPriceTypeAndDate(MEMBERS, PT, D1))
                    .thenReturn(List.of(
                            feature("A", StockExchange.KOSPI, D1, 10.0),     // 상승
                            feature("B", StockExchange.KOSPI, D1, -5.0),     // 하락
                            feature("C", StockExchange.KOSDAQ, D1, 3.0),     // 상승
                            feature("D", StockExchange.KOSDAQ, D1, -1.0)));  // 하락

            service.calculateUniverse(UNIV, PT);

            // union denom=4, num=2 → 0.5, 그리고 KOSPI·KOSDAQ 두 행에 동일 비율
            Map<StockExchange, StockBreadthDaily> byExchange = committedRowsFor(D1).stream()
                    .collect(Collectors.toMap(r -> r.getId().getExchange(), r -> r));
            assertThat(byExchange).containsOnlyKeys(StockExchange.KOSPI, StockExchange.KOSDAQ);
            assertThat(byExchange.get(StockExchange.KOSPI).getAdvanceRatio()).isEqualTo(0.5);
            assertThat(byExchange.get(StockExchange.KOSDAQ).getAdvanceRatio()).isEqualTo(0.5);
        }

        @Test
        @DisplayName("RET_1D가 NULL인 종목은 분모에서 제외한다")
        void shouldExcludeNullRet1dFromDenominator() {
            stubCursor(null, false);
            when(sourceSupport.findIndicatorFrontier(MEMBERS, PT)).thenReturn(D1);
            when(sourceSupport.findFeatureTradeDates(eq(MEMBERS), eq(PT), eq(null), eq(D1)))
                    .thenReturn(List.of(D1));
            when(featureSupport.findByExchangesAndPriceTypeAndDate(MEMBERS, PT, D1))
                    .thenReturn(List.of(
                            feature("A", StockExchange.KOSPI, D1, null),    // 제외
                            feature("B", StockExchange.KOSPI, D1, 10.0),    // 상승
                            feature("C", StockExchange.KOSPI, D1, -5.0)));   // 하락

            service.calculateUniverse(UNIV, PT);

            // denom=2(B,C), num=1(B) → 0.5
            assertThat(committedRowsFor(D1).get(0).getAdvanceRatio()).isEqualTo(0.5);
        }

        @Test
        @DisplayName("RET_1D 값이 하나도 없으면(워밍업) 행 없이 커서만 전진한다")
        void shouldCommitEmptyRowsWhenNoDenominator() {
            stubCursor(null, false);
            when(sourceSupport.findIndicatorFrontier(MEMBERS, PT)).thenReturn(D1);
            when(sourceSupport.findFeatureTradeDates(eq(MEMBERS), eq(PT), eq(null), eq(D1)))
                    .thenReturn(List.of(D1));
            when(featureSupport.findByExchangesAndPriceTypeAndDate(MEMBERS, PT, D1))
                    .thenReturn(List.of(
                            feature("A", StockExchange.KOSPI, D1, null),
                            feature("B", StockExchange.KOSDAQ, D1, null)));

            service.calculateUniverse(UNIV, PT);

            // 빈 목록이라도 커서는 그 날짜로 전진(멈춤 방지)
            verify(commitService).commit(eq(UNIV), eq(PT), eq(List.of()), any(), anyBoolean(), eq(D1));
        }

        @Test
        @DisplayName("커밋 중 커서 rewind가 감지되면 그 universe 계산을 중단한다")
        void shouldStopWhenCursorRewound() {
            stubCursor(null, false);
            when(sourceSupport.findIndicatorFrontier(MEMBERS, PT)).thenReturn(D1.plusDays(2));
            when(sourceSupport.findFeatureTradeDates(eq(MEMBERS), eq(PT), eq(null), eq(D1.plusDays(2))))
                    .thenReturn(List.of(D1, D1.plusDays(1), D1.plusDays(2)));
            when(featureSupport.findByExchangesAndPriceTypeAndDate(eq(MEMBERS), eq(PT), any()))
                    .thenReturn(List.of(feature("A", StockExchange.KOSPI, D1, 10.0)));
            doThrow(new BreadthCursorRewoundException(UNIV, PT))
                    .when(commitService).commit(eq(UNIV), eq(PT), any(), eq(D1), anyBoolean(), eq(D1.plusDays(1)));

            service.calculateUniverse(UNIV, PT);

            verify(commitService, times(2)).commit(any(), any(), any(), any(), anyBoolean(), any());
            verify(commitService, never())
                    .commit(any(), any(), any(), any(), anyBoolean(), eq(D1.plusDays(2)));
        }
    }
}
