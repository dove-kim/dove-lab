package com.dove.indicator.application.service;

import com.dove.indicator.application.exception.RankCursorRewoundException;
import com.dove.indicator.domain.entity.StockFeatureDaily;
import com.dove.indicator.domain.entity.StockFeatureDailyId;
import com.dove.indicator.domain.enums.IndicatorType;
import com.dove.indicator.domain.rank.entity.RankCursor;
import com.dove.indicator.domain.rank.entity.StockRankDaily;
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

class RankCalculationServiceTest {

    private static final MarketUniverse UNIV = MarketUniverse.KRX;
    private static final List<StockExchange> MEMBERS = UNIV.members();
    private static final PriceType PT = PriceType.RAW;
    private static final LocalDate D1 = LocalDate.of(2024, 1, 1);

    private RankCursorService cursorService;
    private RankSourceRepositorySupport sourceSupport;
    private StockFeatureDailyRepositorySupport featureSupport;
    private RankDateCommitService commitService;
    private RankCalculationService service;

    @BeforeEach
    void setUp() {
        cursorService = mock(RankCursorService.class);
        sourceSupport = mock(RankSourceRepositorySupport.class);
        featureSupport = mock(StockFeatureDailyRepositorySupport.class);
        commitService = mock(RankDateCommitService.class);
        service = new RankCalculationService(cursorService, sourceSupport, featureSupport, commitService);
    }

    /** RET_1D 값만 채운 한 종목의 피처 행. */
    private StockFeatureDaily feature(String ticker, StockExchange exchange, LocalDate date,
                                      Double ret1d, Long turnover) {
        StockFeatureDailyId id = new StockFeatureDailyId(ticker, exchange, PT, date);
        StockFeatureDaily row = new StockFeatureDaily(id, 1, 100L, 110L, 90L, 100L, 1000L, turnover,
                LocalDateTime.now());
        if (ret1d != null) row.set(IndicatorType.RET_1D, ret1d);
        return row;
    }

    private void stubCursor(LocalDate cursorDate, boolean exists) {
        Optional<RankCursor> opt;
        if (!exists) {
            opt = Optional.empty();
        } else {
            RankCursor c = new RankCursor(UNIV, PT);
            if (cursorDate != null) c.advance(cursorDate);
            opt = Optional.of(c);
        }
        when(cursorService.findCursor(UNIV, PT)).thenReturn(opt);
    }

    @SuppressWarnings("unchecked")
    private List<StockRankDaily> committedRowsFor(LocalDate date) {
        ArgumentCaptor<List<StockRankDaily>> captor = ArgumentCaptor.forClass(List.class);
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
        @DisplayName("프런티어까지 각 거래일별로 순위를 계산하고 그 날짜로 커서를 전진한다")
        void shouldComputePerDateAndAdvanceCursor() {
            stubCursor(null, false);
            when(sourceSupport.findIndicatorFrontier(MEMBERS, PT)).thenReturn(D1.plusDays(1));
            when(sourceSupport.findFeatureTradeDates(eq(MEMBERS), eq(PT), eq(null), eq(D1.plusDays(1))))
                    .thenReturn(List.of(D1, D1.plusDays(1)));
            when(featureSupport.findByExchangesAndPriceTypeAndDateBetween(eq(MEMBERS), eq(PT), eq(D1), eq(D1.plusDays(1))))
                    .thenReturn(List.of(
                            feature("A", StockExchange.KOSPI, D1, 10.0, 100L),
                            feature("B", StockExchange.KOSPI, D1, 20.0, 200L),
                            feature("C", StockExchange.KOSPI, D1, 30.0, 300L),
                            feature("A", StockExchange.KOSPI, D1.plusDays(1), 5.0, 50L)));

            service.calculateUniverse(UNIV, PT);

            // 날짜별 commit, 각 날짜로 CAS 전진(expected: null→D1)
            ArgumentCaptor<LocalDate> commitDate = ArgumentCaptor.forClass(LocalDate.class);
            verify(commitService, times(2))
                    .commit(eq(UNIV), eq(PT), any(), any(), anyBoolean(), commitDate.capture());
            assertThat(commitDate.getAllValues()).containsExactly(D1, D1.plusDays(1));
        }

        @Test
        @DisplayName("같은 거래일 universe 내에서 PERCENT_RANK(0~1)로 순위를 매긴다")
        void shouldRankWithinDateUniverse() {
            stubCursor(null, false);
            when(sourceSupport.findIndicatorFrontier(MEMBERS, PT)).thenReturn(D1);
            when(sourceSupport.findFeatureTradeDates(eq(MEMBERS), eq(PT), eq(null), eq(D1)))
                    .thenReturn(List.of(D1));
            when(featureSupport.findByExchangesAndPriceTypeAndDateBetween(eq(MEMBERS), eq(PT), eq(D1), eq(D1)))
                    .thenReturn(List.of(
                            feature("A", StockExchange.KOSPI, D1, 10.0, 100L),
                            feature("B", StockExchange.KOSPI, D1, 20.0, 200L),
                            feature("C", StockExchange.KOSPI, D1, 30.0, 300L)));

            service.calculateUniverse(UNIV, PT);

            Map<String, StockRankDaily> byTicker = committedRowsFor(D1).stream()
                    .collect(Collectors.toMap(r -> r.getId().getTicker(), r -> r));
            assertThat(byTicker.get("A").getRankRet1d()).isEqualTo(0.0f);
            assertThat(byTicker.get("B").getRankRet1d()).isEqualTo(0.5f);
            assertThat(byTicker.get("C").getRankRet1d()).isEqualTo(1.0f);
            // TURNOVER도 순위가 매겨진다
            assertThat(byTicker.get("A").getRankTurnover()).isEqualTo(0.0f);
            assertThat(byTicker.get("C").getRankTurnover()).isEqualTo(1.0f);
        }

        @Test
        @DisplayName("KOSPI·KOSDAQ를 풀링해 순위를 매기되 각 행은 native 거래소로 저장한다")
        void shouldPoolMembersButStoreWithNativeExchange() {
            stubCursor(null, false);
            when(sourceSupport.findIndicatorFrontier(MEMBERS, PT)).thenReturn(D1);
            when(sourceSupport.findFeatureTradeDates(eq(MEMBERS), eq(PT), eq(null), eq(D1)))
                    .thenReturn(List.of(D1));
            when(featureSupport.findByExchangesAndPriceTypeAndDateBetween(eq(MEMBERS), eq(PT), eq(D1), eq(D1)))
                    .thenReturn(List.of(
                            feature("A", StockExchange.KOSPI, D1, 10.0, 100L),
                            feature("B", StockExchange.KOSDAQ, D1, 20.0, 200L),
                            feature("C", StockExchange.KOSDAQ, D1, 30.0, 300L)));

            service.calculateUniverse(UNIV, PT);

            Map<String, StockRankDaily> byTicker = committedRowsFor(D1).stream()
                    .collect(Collectors.toMap(r -> r.getId().getTicker(), r -> r));
            // percentile은 KOSPI∪KOSDAQ 3종목 union에서 계산
            assertThat(byTicker.get("A").getRankRet1d()).isEqualTo(0.0f);
            assertThat(byTicker.get("B").getRankRet1d()).isEqualTo(0.5f);
            assertThat(byTicker.get("C").getRankRet1d()).isEqualTo(1.0f);
            // 행은 종목의 native 거래소로 키됨
            assertThat(byTicker.get("A").getId().getExchange()).isEqualTo(StockExchange.KOSPI);
            assertThat(byTicker.get("B").getId().getExchange()).isEqualTo(StockExchange.KOSDAQ);
            assertThat(byTicker.get("C").getId().getExchange()).isEqualTo(StockExchange.KOSDAQ);
        }

        @Test
        @DisplayName("원천 값이 NULL인 종목은 그 순위 컬럼이 NULL이다")
        void shouldLeaveRankNullWhenSourceNull() {
            stubCursor(null, false);
            when(sourceSupport.findIndicatorFrontier(MEMBERS, PT)).thenReturn(D1);
            when(sourceSupport.findFeatureTradeDates(eq(MEMBERS), eq(PT), eq(null), eq(D1)))
                    .thenReturn(List.of(D1));
            when(featureSupport.findByExchangesAndPriceTypeAndDateBetween(eq(MEMBERS), eq(PT), eq(D1), eq(D1)))
                    .thenReturn(List.of(
                            feature("A", StockExchange.KOSPI, D1, null, 100L),   // RET_1D 없음
                            feature("B", StockExchange.KOSPI, D1, 20.0, 200L),
                            feature("C", StockExchange.KOSPI, D1, 30.0, 300L)));

            service.calculateUniverse(UNIV, PT);

            Map<String, StockRankDaily> byTicker = committedRowsFor(D1).stream()
                    .collect(Collectors.toMap(r -> r.getId().getTicker(), r -> r));
            assertThat(byTicker.get("A").getRankRet1d()).isNull();          // NULL 원천 → 순위 제외
            assertThat(byTicker.get("B").getRankRet1d()).isEqualTo(0.0f);   // B,C만으로 순위
            assertThat(byTicker.get("C").getRankRet1d()).isEqualTo(1.0f);
        }

        @Test
        @DisplayName("커밋 중 커서 rewind가 감지되면 그 universe 계산을 중단한다")
        void shouldStopWhenCursorRewound() {
            stubCursor(null, false);
            when(sourceSupport.findIndicatorFrontier(MEMBERS, PT)).thenReturn(D1.plusDays(2));
            when(sourceSupport.findFeatureTradeDates(eq(MEMBERS), eq(PT), eq(null), eq(D1.plusDays(2))))
                    .thenReturn(List.of(D1, D1.plusDays(1), D1.plusDays(2)));
            when(featureSupport.findByExchangesAndPriceTypeAndDateBetween(eq(MEMBERS), eq(PT), eq(D1), eq(D1.plusDays(2))))
                    .thenReturn(List.of(
                            feature("A", StockExchange.KOSPI, D1, 10.0, 100L),
                            feature("A", StockExchange.KOSPI, D1.plusDays(1), 10.0, 100L),
                            feature("A", StockExchange.KOSPI, D1.plusDays(2), 10.0, 100L)));
            // 두 번째 날짜 커밋에서 rewind 발생
            doThrow(new RankCursorRewoundException(UNIV, PT))
                    .when(commitService).commit(eq(UNIV), eq(PT), any(), eq(D1), anyBoolean(), eq(D1.plusDays(1)));

            service.calculateUniverse(UNIV, PT);

            // D1(성공), D1+1(rewind) 두 번만 시도하고 D1+2는 시도하지 않음
            verify(commitService, times(2)).commit(any(), any(), any(), any(), anyBoolean(), any());
            verify(commitService, never())
                    .commit(any(), any(), any(), any(), anyBoolean(), eq(D1.plusDays(2)));
        }
    }
}
