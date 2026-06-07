package com.dove.indicator.application.service;

import com.dove.indicator.domain.calculator.EmaCalculator;
import com.dove.indicator.domain.calculator.ObvCalculator;
import com.dove.indicator.domain.calculator.SmaCalculator;
import com.dove.indicator.domain.calculator.TechnicalIndicatorCalculator;
import com.dove.indicator.domain.entity.IndicatorCursor;
import com.dove.indicator.domain.entity.StockFeatureDaily;
import com.dove.indicator.domain.enums.IndicatorType;
import com.dove.stock.application.service.StockPriceQueryService;
import com.dove.stock.domain.entity.StockPrice;
import com.dove.stock.domain.enums.PriceType;
import com.dove.stock.domain.enums.StockExchange;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class IndicatorBulkCalculateServiceTest {

    private static final LocalDate D1 = LocalDate.of(2024, 1, 1);
    private static final String TICKER = "005930";
    private static final StockExchange EX = StockExchange.KOSPI;
    private static final PriceType PT = PriceType.RAW;

    private StockPriceQueryService priceQueryService;
    private IndicatorCursorService cursorService;
    private IndicatorChunkCommitService commitService;
    private StockFeatureDailyService featureService;
    private Map<LocalDate, StockFeatureDaily> committedStore;
    private IndicatorBulkCalculateService service;

    @BeforeEach
    void setUp() {
        priceQueryService = mock(StockPriceQueryService.class);
        cursorService = mock(IndicatorCursorService.class);
        commitService = mock(IndicatorChunkCommitService.class);
        featureService = mock(StockFeatureDailyService.class);
        committedStore = new HashMap<>();
        // commit된 행을 인메모리에 저장 → 다음 청크가 직전 거래일의 저장값을 시드로 읽도록 한다.
        doAnswer(inv -> {
            List<StockFeatureDaily> rows = inv.getArgument(3);
            rows.forEach(r -> committedStore.put(r.getId().getTradeDate(), r));
            return null;
        }).when(commitService).commit(any(), any(), any(), any(), any(), anyBoolean(), any());
        when(featureService.findIndicatorsByDate(eq(TICKER), eq(EX), eq(PT), any()))
                .thenAnswer(inv -> {
                    StockFeatureDaily r = committedStore.get(inv.getArgument(3, LocalDate.class));
                    return r == null ? Map.of() : r.toIndicatorMap();
                });
        service = build(List.of(
                new SmaCalculator(5, IndicatorType.SMA_5),   // 비누적
                new EmaCalculator(5, IndicatorType.EMA_5))); // 누적(감쇠)
    }

    private IndicatorBulkCalculateService build(List<TechnicalIndicatorCalculator> calculators) {
        return new IndicatorBulkCalculateService(
                priceQueryService, cursorService, commitService, featureService, calculators);
    }

    /** day=1..n 의 연속 거래일 주가 (종가=100,101,...). */
    private List<StockPrice> prices(int n) {
        List<StockPrice> list = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            list.add(new StockPrice(TICKER, EX, PT, D1.plusDays(i), 100L, 110L, 90L, 100L + i, 1000L, 0L));
        }
        return list;
    }

    /** priceQueryService를 인메모리 리스트 기반으로 스텁한다 (findChunk=오름차순 페이징, findBefore=lookback). */
    private void stubPrices(List<StockPrice> all, LocalDate today) {
        when(priceQueryService.findChunk(eq(TICKER), eq(EX), eq(PT), any(), eq(today), anyInt()))
                .thenAnswer(inv -> {
                    LocalDate from = inv.getArgument(3);
                    int limit = inv.getArgument(5);
                    return all.stream()
                            .filter(p -> !p.getTradeDate().isBefore(from) && !p.getTradeDate().isAfter(today))
                            .limit(limit).toList();
                });
        when(priceQueryService.findBefore(eq(TICKER), eq(EX), eq(PT), any(), anyInt()))
                .thenAnswer(inv -> {
                    LocalDate before = inv.getArgument(3);
                    int limit = inv.getArgument(4);
                    List<StockPrice> desc = all.stream()
                            .filter(p -> p.getTradeDate().isBefore(before))
                            .sorted((a, b) -> b.getTradeDate().compareTo(a.getTradeDate()))
                            .limit(limit).collect(Collectors.toList());
                    java.util.Collections.reverse(desc);
                    return desc;
                });
    }

    /** 그룹 커서가 cursorDate에 있도록 스텁(없으면 null). 계산 시작일은 IndicatorCursor.firstSaveDate(=cursor+1)로 결정된다. */
    private void stubCursor(LocalDate cursorDate) {
        Optional<IndicatorCursor> opt = cursorDate == null
                ? Optional.empty()
                : Optional.of(cursorWith(cursorDate));
        when(cursorService.findCursor(TICKER, EX, PT)).thenReturn(opt);
    }

    private IndicatorCursor cursorWith(LocalDate date) {
        IndicatorCursor c = new IndicatorCursor(TICKER, EX, PT);
        c.advance(date);
        return c;
    }

    /** commit에 전달된 wide 행을 거래일별 "값이 채워진 지표 집합"으로 합친다. */
    private Map<LocalDate, Set<IndicatorType>> capturedByDate() {
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<StockFeatureDaily>> captor = ArgumentCaptor.forClass(List.class);
        verify(commitService, atLeastOnce())
                .commit(any(), any(), any(), captor.capture(), any(), anyBoolean(), any());
        return captor.getAllValues().stream().flatMap(List::stream)
                .collect(Collectors.toMap(r -> r.getId().getTradeDate(), this::presentIndicators));
    }

    private Set<IndicatorType> presentIndicators(StockFeatureDaily row) {
        Set<IndicatorType> present = EnumSet.noneOf(IndicatorType.class);
        if (row.getSma5() != null) present.add(IndicatorType.SMA_5);
        if (row.getEma5() != null) present.add(IndicatorType.EMA_5);
        return present;
    }

    /** commit에 전달된 행들의 거래일별 EMA_5(누적 지표) 값. */
    private Map<LocalDate, Float> capturedEma5() {
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<StockFeatureDaily>> captor = ArgumentCaptor.forClass(List.class);
        verify(commitService, atLeastOnce())
                .commit(any(), any(), any(), captor.capture(), any(), anyBoolean(), any());
        Map<LocalDate, Float> m = new TreeMap<>();
        captor.getAllValues().stream().flatMap(List::stream)
                .filter(r -> r.getEma5() != null)
                .forEach(r -> m.put(r.getId().getTradeDate(), r.getEma5()));
        return m;
    }

    /** commit에 전달된 행들의 거래일별 OBV(비감쇠 누적 지표) 값. */
    private Map<LocalDate, Float> capturedObv() {
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<StockFeatureDaily>> captor = ArgumentCaptor.forClass(List.class);
        verify(commitService, atLeastOnce())
                .commit(any(), any(), any(), captor.capture(), any(), anyBoolean(), any());
        Map<LocalDate, Float> m = new TreeMap<>();
        captor.getAllValues().stream().flatMap(List::stream)
                .filter(r -> r.getObv() != null)
                .forEach(r -> m.put(r.getId().getTradeDate(), r.getObv()));
        return m;
    }

    @Test
    @DisplayName("청크 경계를 넘어도 비감쇠 누적 지표(OBV)가 직전 저장값을 시드로 연속 계산된다 (chunkSize=7)")
    void shouldKeepObvContinuityAcrossChunksViaPersistedSeed() {
        IndicatorBulkCalculateService obvService = build(List.of(new ObvCalculator()));
        LocalDate today = D1.plusDays(19); // D20
        stubPrices(prices(20), today);
        stubCursor(null);
        ReflectionTestUtils.setField(obvService, "chunkSize", 7); // 20행 → 7/7/6 (경계 2회)

        obvService.calculateGroup(TICKER, EX, PT, today);

        // 종가가 매일 상승 → OBV는 매일 +거래량(1000). D2부터 (k-1)*1000, 청크 경계서도 끊김 없이 연속.
        Map<LocalDate, Float> obv = capturedObv();
        assertThat(obv.get(D1)).isNull();                       // 첫 행은 직전 종가 없음 → NULL
        assertThat(obv.get(D1.plusDays(1))).isEqualTo(1000.0f); // D2 첫 OBV
        assertThat(obv.get(D1.plusDays(7))).isEqualTo(7000.0f); // D8 chunk2 첫 행(직전 저장값 시드)
        assertThat(obv.get(D1.plusDays(14))).isEqualTo(14000.0f); // D15 chunk3 첫 행
        assertThat(obv.get(D1.plusDays(19))).isEqualTo(19000.0f); // D20 마지막
    }

    @Test
    @DisplayName("청크 경계를 넘어도 누적 지표(EMA_5)가 연속 계산된다 (chunkSize=7, golden)")
    void shouldKeepCumulativeContinuityAcrossChunks() {
        LocalDate today = D1.plusDays(19); // D20
        stubPrices(prices(20), today);
        stubCursor(null);
        ReflectionTestUtils.setField(service, "chunkSize", 7); // 20행 → 7/7/6 (경계 2회)

        service.calculateGroup(TICKER, EX, PT, today);

        // golden: 청크 경계(D8=chunk2 첫행, D15=chunk3 첫행)를 포함해 lookback 재시드 결과를 고정
        Map<LocalDate, Float> ema = capturedEma5();
        assertThat(ema.get(D1.plusDays(4))).isEqualTo(102.0f);   // D5 첫 EMA
        assertThat(ema.get(D1.plusDays(7))).isEqualTo(105.0f);   // D8 chunk2 첫 행(lookback 재시드)
        assertThat(ema.get(D1.plusDays(14))).isEqualTo(112.0f);  // D15 chunk3 첫 행
        assertThat(ema.get(D1.plusDays(19))).isEqualTo(117.0f);  // D20 마지막
    }

    @Test
    @DisplayName("saveFrom 이전 날짜는 저장하지 않는다 (한 날짜=한 행)")
    void shouldSaveOnlyFromCursor() {
        LocalDate today = D1.plusDays(11);    // D12
        stubPrices(prices(12), today);
        stubCursor(D1.plusDays(8)); // 커서 D9 → from D10 (firstSaveDate = cursor+1)

        service.calculateGroup(TICKER, EX, PT, today);

        Map<LocalDate, Set<IndicatorType>> byDate = capturedByDate();
        assertThat(byDate.keySet()).containsExactlyInAnyOrder(
                D1.plusDays(9), D1.plusDays(10), D1.plusDays(11));
        assertThat(byDate.get(D1.plusDays(9))).containsExactlyInAnyOrder(IndicatorType.SMA_5, IndicatorType.EMA_5);
    }

    @Test
    @DisplayName("warmup 미달 지표는 그 행에서 NULL (한 날짜=한 행, 점진적 채움)")
    void shouldLeaveNullUntilWarmup() {
        LocalDate today = D1.plusDays(11); // D12
        stubPrices(prices(12), today);
        stubCursor(null); // 커서 없음 → from D1

        service.calculateGroup(TICKER, EX, PT, today);

        Map<LocalDate, Set<IndicatorType>> byDate = capturedByDate();
        // 모든 거래일에 행이 생기지만, warmup(5행) 전엔 두 지표 모두 NULL
        assertThat(byDate.keySet()).hasSize(12);
        assertThat(byDate.get(D1)).isEmpty();
        assertThat(byDate.get(D1.plusDays(3))).isEmpty();              // D4
        assertThat(byDate.get(D1.plusDays(4)))                        // D5 첫 풀 완성
                .containsExactlyInAnyOrder(IndicatorType.SMA_5, IndicatorType.EMA_5);
    }

    @Test
    @DisplayName("today 이후 날짜는 저장하지 않는다")
    void shouldNotSaveAfterToday() {
        LocalDate today = D1.plusDays(8); // D9까지만
        stubPrices(prices(12), today);
        stubCursor(null);

        service.calculateGroup(TICKER, EX, PT, today);

        Map<LocalDate, Set<IndicatorType>> byDate = capturedByDate();
        assertThat(byDate.keySet()).allSatisfy(d -> assertThat(d).isBeforeOrEqualTo(today));
    }

    @Test
    @DisplayName("100행 청크마다 commit 하고, 청크 끝 날짜로 커서를 전진한다")
    void shouldCommitPerChunkOf100() {
        LocalDate today = D1.plusDays(249); // D250
        stubPrices(prices(250), today);
        stubCursor(null);

        service.calculateGroup(TICKER, EX, PT, today);

        // 250행 → 100/100/50 → 3회 commit, 각 청크 끝 날짜로 전진
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<StockFeatureDaily>> rows = ArgumentCaptor.forClass(List.class);
        ArgumentCaptor<LocalDate> chunkEnd = ArgumentCaptor.forClass(LocalDate.class);
        verify(commitService, times(3))
                .commit(any(), any(), any(), rows.capture(), any(), anyBoolean(), chunkEnd.capture());
        assertThat(chunkEnd.getAllValues())
                .containsExactly(D1.plusDays(99), D1.plusDays(199), D1.plusDays(249));

        // SMA_5 값이 채워진 행은 D5~D250 각 1회만(청크 경계 중복 없음, 누락 없음).
        List<LocalDate> smaDates = rows.getAllValues().stream().flatMap(List::stream)
                .filter(r -> r.getSma5() != null)
                .map(r -> r.getId().getTradeDate()).sorted().toList();
        List<LocalDate> expected = new ArrayList<>();
        for (int d = 4; d <= 249; d++) expected.add(D1.plusDays(d)); // 첫 풀 완성 D5 ~ D250
        assertThat(smaDates).isEqualTo(expected);
    }
}
