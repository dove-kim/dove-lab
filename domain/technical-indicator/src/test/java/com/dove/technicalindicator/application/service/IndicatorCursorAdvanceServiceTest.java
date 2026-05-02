package com.dove.technicalindicator.application.service;

import com.dove.market.application.service.MarketTradingDateQueryService;
import com.dove.market.domain.enums.MarketType;
import com.dove.stockprice.application.service.DailyStockPriceQueryService;
import com.dove.stockprice.domain.entity.DailyStockPrice;
import com.dove.technicalindicator.domain.calculator.TechnicalIndicatorCalculator;
import com.dove.technicalindicator.domain.entity.IndicatorCursor;
import com.dove.technicalindicator.domain.entity.IndicatorCursorId;
import com.dove.technicalindicator.domain.enums.DateState;
import com.dove.technicalindicator.domain.enums.IndicatorType;
import com.dove.technicalindicator.domain.repository.TechnicalIndicatorRepository;
import com.dove.technicalindicator.domain.service.IndicatorDateClassifier;
import com.dove.technicalindicator.domain.service.LookbackCollector;
import com.dove.technicalindicator.infrastructure.config.IndicatorCursorProperties;
import com.dove.technicalindicator.infrastructure.repository.TechnicalIndicatorQueryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class IndicatorCursorAdvanceServiceTest {

    @Mock private TechnicalIndicatorCalculator calculator;
    @Mock private IndicatorCursorCommandService cursorCommandService;
    @Mock private DailyStockPriceQueryService dailyStockPriceQueryService;
    @Mock private MarketTradingDateQueryService marketTradingDateQueryService;
    @Mock private IndicatorDateClassifier dateClassifier;
    @Mock private LookbackCollector lookbackCollector;
    @Mock private TechnicalIndicatorRepository technicalIndicatorRepository;
    @Mock private TechnicalIndicatorQueryRepository indicatorQueryRepository;

    private IndicatorCursorAdvanceService service;

    private static final MarketType MARKET = MarketType.KOSPI;
    private static final String CODE = "005930";
    private static final LocalDate INITIAL = LocalDate.of(2010, 1, 1);
    private static final LocalDate LAST_CALC = LocalDate.of(2024, 1, 4);
    private static final LocalDate NEXT_DAY = LocalDate.of(2024, 1, 5);

    @BeforeEach
    void setUp() {
        IndicatorCursorProperties props = new IndicatorCursorProperties();
        props.setInitialDate(INITIAL);
        service = new IndicatorCursorAdvanceService(
                List.of(calculator), cursorCommandService, dailyStockPriceQueryService,
                marketTradingDateQueryService, dateClassifier, lookbackCollector,
                technicalIndicatorRepository, indicatorQueryRepository, props);
    }

    private IndicatorCursor stubCursor(IndicatorType type, LocalDate lastCalc) {
        IndicatorCursor cursor = new IndicatorCursor(
                new IndicatorCursorId(MARKET, CODE, type), lastCalc);
        given(cursorCommandService.getOrCreate(eq(MARKET), eq(CODE), eq(type), any()))
                .willReturn(cursor);
        return cursor;
    }

    private DailyStockPrice price(LocalDate date) {
        return new DailyStockPrice(MARKET, CODE, date, 1000L, 1000L, 1000L, 1000L, 1000L);
    }

    /** findCalculator() 호출 전제 — latestOpt present + existsOpenDay true */
    private void stubTradingDay(LocalDate date) {
        given(dailyStockPriceQueryService.findLatestTradeDateByMarket(MARKET)).willReturn(Optional.of(date));
        given(marketTradingDateQueryService.existsOpenDay(MARKET, date)).willReturn(true);
    }

    @Test
    @DisplayName("ACTIVE: 풀 데이터 충분 → 계산 결과 저장 + cursor 전진 + 다음 날짜 반환")
    void shouldCalculateAndAdvanceOnActive() {
        given(calculator.cursorType()).willReturn(IndicatorType.SMA_5);
        given(calculator.requiredDataSize()).willReturn(2);
        IndicatorCursor cursor = stubCursor(IndicatorType.SMA_5, LAST_CALC);
        stubTradingDay(NEXT_DAY);
        given(dateClassifier.classify(MARKET, CODE, NEXT_DAY)).willReturn(DateState.ACTIVE);

        List<DailyStockPrice> pool = List.of(price(LAST_CALC), price(NEXT_DAY));
        given(lookbackCollector.collect(MARKET, CODE, NEXT_DAY, 2)).willReturn(pool);
        given(calculator.calculateWithSeed(eq(pool), anyDouble()))
                .willReturn(Map.of(IndicatorType.SMA_5, 1000.0));

        Optional<LocalDate> result = service.advance(MARKET, CODE, IndicatorType.SMA_5, null);

        assertThat(result).isPresent();
        assertThat(cursor.getLastCalculatedDate()).isEqualTo(NEXT_DAY);
        verify(technicalIndicatorRepository).saveAll(any());
    }

    @Test
    @DisplayName("ACTIVE: lookback 부족 → 저장 없음, cursor 전진, 다음 날짜 반환")
    void shouldAdvanceCursorWithoutSavingWhenLookbackInsufficient() {
        given(calculator.cursorType()).willReturn(IndicatorType.SMA_5);
        given(calculator.requiredDataSize()).willReturn(5);
        IndicatorCursor cursor = stubCursor(IndicatorType.SMA_5, LAST_CALC);
        stubTradingDay(NEXT_DAY);
        given(dateClassifier.classify(MARKET, CODE, NEXT_DAY)).willReturn(DateState.ACTIVE);
        given(lookbackCollector.collect(MARKET, CODE, NEXT_DAY, 5))
                .willReturn(List.of(price(NEXT_DAY)));

        Optional<LocalDate> result = service.advance(MARKET, CODE, IndicatorType.SMA_5, null);

        assertThat(result).isPresent();
        assertThat(cursor.getLastCalculatedDate()).isEqualTo(NEXT_DAY);
        verify(technicalIndicatorRepository, never()).saveAll(any());
    }

    @Test
    @DisplayName("SUSPENDED: 저장 없음, cursor 전진, 다음 날짜 반환")
    void shouldAdvanceCursorOnSuspended() {
        given(calculator.cursorType()).willReturn(IndicatorType.SMA_5);
        IndicatorCursor cursor = stubCursor(IndicatorType.SMA_5, LAST_CALC);
        stubTradingDay(NEXT_DAY);
        given(dateClassifier.classify(MARKET, CODE, NEXT_DAY)).willReturn(DateState.SUSPENDED);

        Optional<LocalDate> result = service.advance(MARKET, CODE, IndicatorType.SMA_5, null);

        assertThat(result).isPresent();
        assertThat(cursor.getLastCalculatedDate()).isEqualTo(NEXT_DAY);
        verify(lookbackCollector, never()).collect(any(), any(), any(), anyInt());
    }

    @Test
    @DisplayName("DELISTED: 저장 없음, cursor 전진, 다음 날짜 반환")
    void shouldAdvanceCursorOnDelisted() {
        given(calculator.cursorType()).willReturn(IndicatorType.SMA_5);
        IndicatorCursor cursor = stubCursor(IndicatorType.SMA_5, LAST_CALC);
        stubTradingDay(NEXT_DAY);
        given(dateClassifier.classify(MARKET, CODE, NEXT_DAY)).willReturn(DateState.DELISTED);

        Optional<LocalDate> result = service.advance(MARKET, CODE, IndicatorType.SMA_5, null);

        assertThat(result).isPresent();
        assertThat(cursor.getLastCalculatedDate()).isEqualTo(NEXT_DAY);
    }

    @Test
    @DisplayName("LISTING_NOT_SYNCED: cursor 멈춤, empty 반환")
    void shouldStopCursorOnListingNotSynced() {
        given(calculator.cursorType()).willReturn(IndicatorType.SMA_5);
        IndicatorCursor cursor = stubCursor(IndicatorType.SMA_5, LAST_CALC);
        stubTradingDay(NEXT_DAY);
        given(dateClassifier.classify(MARKET, CODE, NEXT_DAY)).willReturn(DateState.LISTING_NOT_SYNCED);

        Optional<LocalDate> result = service.advance(MARKET, CODE, IndicatorType.SMA_5, null);

        assertThat(result).isEmpty();
        assertThat(cursor.getLastCalculatedDate()).isEqualTo(LAST_CALC);
    }

    @Test
    @DisplayName("휴장일 — latestPriceDate >= nextDay이지만 해당 날짜 데이터 없음 → cursor 전진(계산 없음), 다음 날짜 반환")
    void shouldAdvanceCursorOnHolidayWithoutCalculating() {
        given(calculator.cursorType()).willReturn(IndicatorType.SMA_5);
        IndicatorCursor cursor = stubCursor(IndicatorType.SMA_5, LAST_CALC);
        given(dailyStockPriceQueryService.findLatestTradeDateByMarket(MARKET))
                .willReturn(Optional.of(NEXT_DAY));
        given(marketTradingDateQueryService.existsOpenDay(MARKET, NEXT_DAY))
                .willReturn(false);

        Optional<LocalDate> result = service.advance(MARKET, CODE, IndicatorType.SMA_5, null);

        assertThat(result).isPresent();
        assertThat(cursor.getLastCalculatedDate()).isEqualTo(NEXT_DAY);
        verify(dateClassifier, never()).classify(any(), any(), any());
        verify(technicalIndicatorRepository, never()).saveAll(any());
    }

    @Test
    @DisplayName("데이터 미도달 — latestPriceDate < nextDay → cursor 정지, empty 반환")
    void shouldStopWhenDataNotYetAvailable() {
        given(calculator.cursorType()).willReturn(IndicatorType.SMA_5);
        stubCursor(IndicatorType.SMA_5, LAST_CALC);
        given(dailyStockPriceQueryService.findLatestTradeDateByMarket(MARKET))
                .willReturn(Optional.of(LAST_CALC));

        Optional<LocalDate> result = service.advance(MARKET, CODE, IndicatorType.SMA_5, null);

        assertThat(result).isEmpty();
        verify(dailyStockPriceQueryService, never()).existsByMarketAndDate(any(), any());
    }

    @Test
    @DisplayName("가격 데이터 전혀 없음 → 즉시 empty 반환")
    void shouldReturnEmptyWhenNoPriceDataAtAll() {
        // findCalculator() 도달 전 latestOpt.isEmpty() 로 early return → cursorType 스텁 불필요
        stubCursor(IndicatorType.SMA_5, LAST_CALC);
        given(dailyStockPriceQueryService.findLatestTradeDateByMarket(MARKET))
                .willReturn(Optional.empty());

        Optional<LocalDate> result = service.advance(MARKET, CODE, IndicatorType.SMA_5, null);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("OBV calculator는 seed를 주입하여 calculateWithSeed 호출")
    void shouldInjectObvSeedForObvCalculator() {
        given(calculator.cursorType()).willReturn(IndicatorType.OBV);
        given(calculator.requiredDataSize()).willReturn(2);
        stubCursor(IndicatorType.OBV, LAST_CALC);
        stubTradingDay(NEXT_DAY);
        given(dateClassifier.classify(MARKET, CODE, NEXT_DAY)).willReturn(DateState.ACTIVE);

        List<DailyStockPrice> pool = List.of(price(LAST_CALC), price(NEXT_DAY));
        given(lookbackCollector.collect(MARKET, CODE, NEXT_DAY, 2)).willReturn(pool);
        given(indicatorQueryRepository.findLatestObvValue(eq(MARKET), eq(CODE), any(), any()))
                .willReturn(Optional.of(50000.0));
        given(calculator.calculateWithSeed(eq(pool), eq(50000.0)))
                .willReturn(Map.of(IndicatorType.OBV, 51000.0));

        service.advance(MARKET, CODE, IndicatorType.OBV, null);

        verify(calculator).calculateWithSeed(pool, 50000.0);
    }

    @Test
    @DisplayName("insertedDate < cursorNext(과거 변경) → rewindIfBefore 호출")
    void shouldRewindCursorWhenInsertedDateIsBeforeCursorNext() {
        // insertedDate(1/3) < nextDay(1/5) → rewind → 이후 latestOpt empty로 early return
        // findCalculator() 미도달 → cursorType 스텁 불필요
        LocalDate insertedDate = LocalDate.of(2024, 1, 3);
        stubCursor(IndicatorType.SMA_5, LAST_CALC);
        given(dailyStockPriceQueryService.findLatestTradeDateByMarket(MARKET))
                .willReturn(Optional.empty());

        service.advance(MARKET, CODE, IndicatorType.SMA_5, insertedDate);

        verify(cursorCommandService).rewindIfBefore(MARKET, CODE, IndicatorType.SMA_5, insertedDate);
    }

    @Test
    @DisplayName("insertedDate > cursorNext(커서 미도달) → 즉시 empty 반환, rewind 없음")
    void shouldIgnoreWhenInsertedDateIsAheadOfCursorNext() {
        // insertedDate(1/10) > nextDay(1/5) → early return 전 findCalculator 미도달 → cursorType 스텁 불필요
        LocalDate futureDate = LocalDate.of(2024, 1, 10);
        stubCursor(IndicatorType.SMA_5, LAST_CALC);

        Optional<LocalDate> result = service.advance(MARKET, CODE, IndicatorType.SMA_5, futureDate);

        assertThat(result).isEmpty();
        verify(cursorCommandService, never()).rewindIfBefore(any(), any(), any(), any());
        verify(dailyStockPriceQueryService, never()).findLatestTradeDateByMarket(any());
    }

    @Test
    @DisplayName("insertedDate == cursorNext → rewind 없이 정상 진행")
    void shouldProceedNormallyWhenInsertedDateMatchesCursorNext() {
        // insertedDate(1/5) == nextDay(1/5) → 정상 진행, latestOpt empty 로 early return
        // findCalculator() 미도달 → cursorType 스텁 불필요
        stubCursor(IndicatorType.SMA_5, LAST_CALC);
        given(dailyStockPriceQueryService.findLatestTradeDateByMarket(MARKET))
                .willReturn(Optional.empty());

        service.advance(MARKET, CODE, IndicatorType.SMA_5, NEXT_DAY);

        verify(cursorCommandService, never()).rewindIfBefore(any(), any(), any(), any());
    }

    @Test
    @DisplayName("insertedDate null → rewindIfBefore 호출 없음")
    void shouldNotRewindWhenInsertedDateIsNull() {
        // findCalculator() 미도달 → cursorType 스텁 불필요
        stubCursor(IndicatorType.SMA_5, LAST_CALC);
        given(dailyStockPriceQueryService.findLatestTradeDateByMarket(MARKET))
                .willReturn(Optional.empty());

        service.advance(MARKET, CODE, IndicatorType.SMA_5, null);

        verify(cursorCommandService, never()).rewindIfBefore(any(), any(), any(), any());
    }
}
