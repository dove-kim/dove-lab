package com.dove.stockbatch.producer;

import com.dove.market.domain.enums.MarketType;
import com.dove.market.application.dto.DailyMarketDataQuery;
import com.dove.market.application.service.MarketDataCursorQueryService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class DailyMarketDataProcessorTest {

    private static final String TOPIC = DailyMarketDataQuery.TOPIC;
    private static final LocalDate INITIAL_DATE = LocalDate.of(2010, 1, 1);
    private static final List<MarketType> DEFAULT_MARKETS = List.of(MarketType.KOSPI, MarketType.KOSDAQ);

    @SuppressWarnings("unchecked")
    private final KafkaTemplate<String, Object> kafkaTemplate = mock(KafkaTemplate.class);
    private final MarketDataCursorQueryService cursorQueryService = mock(MarketDataCursorQueryService.class);

    private DailyMarketDataProcessor processorFor(LocalDate pretendToday, List<MarketType> markets) {
        Clock clock = Clock.fixed(pretendToday.atStartOfDay(ZoneOffset.UTC).toInstant(), ZoneId.of("UTC"));
        return new DailyMarketDataProcessor(kafkaTemplate, cursorQueryService, clock, markets, INITIAL_DATE);
    }

    @Test
    @DisplayName("커서 없음 → initial-date부터 lastAvailableDate까지 발행 (토·일 포함)")
    void shouldPublishFromInitialDateWhenNoCursor() {
        // 2010-01-06(수): lastAvailableDate=화01/05, from=01/01 → 5건
        LocalDate today = LocalDate.of(2010, 1, 6);
        when(cursorQueryService.findLastProcessedDate(any())).thenReturn(Optional.empty());

        processorFor(today, List.of(MarketType.KOSPI)).run();

        verify(kafkaTemplate, times(5)).send(eq(TOPIC), any(), any());
    }

    @Test
    @DisplayName("커서 있음 → 다음날부터 lastAvailableDate까지 발행")
    void shouldPublishFromDayAfterCursor() {
        // today=목(04/23): lastAvailableDate=화04/22, cursor=04/20 → 04/21, 04/22 = 2건
        LocalDate today = LocalDate.of(2026, 4, 23);
        when(cursorQueryService.findLastProcessedDate(MarketType.KOSPI))
                .thenReturn(Optional.of(LocalDate.of(2026, 4, 20)));

        processorFor(today, List.of(MarketType.KOSPI)).run();

        ArgumentCaptor<DailyMarketDataQuery> captor = ArgumentCaptor.forClass(DailyMarketDataQuery.class);
        verify(kafkaTemplate, times(2)).send(eq(TOPIC), any(), captor.capture());
        assertThat(captor.getAllValues())
                .extracting(DailyMarketDataQuery::getBaseDate)
                .containsExactly(LocalDate.of(2026, 4, 21), LocalDate.of(2026, 4, 22));
    }

    @Test
    @DisplayName("커서가 이미 최신 → 발행 없음")
    void shouldSkipWhenAlreadyUpToDate() {
        // today=화(04/22): lastAvailableDate=월04/21, cursor=04/21 → skip
        LocalDate today = LocalDate.of(2026, 4, 22);
        when(cursorQueryService.findLastProcessedDate(any()))
                .thenReturn(Optional.of(LocalDate.of(2026, 4, 21)));

        processorFor(today, DEFAULT_MARKETS).run();

        verify(kafkaTemplate, never()).send(any(), any(), any());
    }

    @Test
    @DisplayName("화요일 실행 — 직전 토·일 포함 발행")
    void shouldIncludeWeekendDates() {
        // today=화(04/28): lastAvailableDate=월04/27, cursor=금04/24 → 04/25(토), 04/26(일), 04/27(월) = 3건
        LocalDate today = LocalDate.of(2026, 4, 28);
        when(cursorQueryService.findLastProcessedDate(MarketType.KOSPI))
                .thenReturn(Optional.of(LocalDate.of(2026, 4, 24)));

        processorFor(today, List.of(MarketType.KOSPI)).run();

        verify(kafkaTemplate, times(3)).send(eq(TOPIC), any(), any());
    }

    @Test
    @DisplayName("연휴 이후 — 휴장일 포함 전체 범위 발행")
    void shouldPublishAllDatesIncludingHolidaysAfterLongWeekend() {
        // today=월(05/04): lastAvailableDate=금05/01, cursor=월04/27 → 04/28~05/01 = 4건
        LocalDate today = LocalDate.of(2026, 5, 4);
        when(cursorQueryService.findLastProcessedDate(MarketType.KOSPI))
                .thenReturn(Optional.of(LocalDate.of(2026, 4, 27)));

        processorFor(today, List.of(MarketType.KOSPI)).run();

        verify(kafkaTemplate, times(4)).send(eq(TOPIC), any(), any());
    }

    @Test
    @DisplayName("파티션 키: 시장명 단위 — 같은 시장의 모든 날짜가 동일 파티션으로 전송됨")
    void shouldUseMarketAsPartitionKey() {
        LocalDate today = LocalDate.of(2026, 4, 23);
        when(cursorQueryService.findLastProcessedDate(MarketType.KOSPI))
                .thenReturn(Optional.of(LocalDate.of(2026, 4, 21)));

        processorFor(today, List.of(MarketType.KOSPI)).run();

        verify(kafkaTemplate).send(eq(TOPIC), eq("KOSPI"), any());
    }

    @Test
    @DisplayName("여러 시장: 각 시장 독립적으로 커서 기준 계산")
    void shouldCalculateRangeIndependentlyPerMarket() {
        // today=목(04/23): KOSPI cursor=04/21 → 1건, KOSDAQ cursor=04/20 → 2건
        LocalDate today = LocalDate.of(2026, 4, 23);
        when(cursorQueryService.findLastProcessedDate(MarketType.KOSPI))
                .thenReturn(Optional.of(LocalDate.of(2026, 4, 21)));
        when(cursorQueryService.findLastProcessedDate(MarketType.KOSDAQ))
                .thenReturn(Optional.of(LocalDate.of(2026, 4, 20)));

        processorFor(today, DEFAULT_MARKETS).run();

        verify(kafkaTemplate, times(3)).send(eq(TOPIC), any(), any());
    }

    @Test
    @DisplayName("Kafka 전송 실패 시 나머지 날짜·시장 계속 전송")
    void shouldContinueOnKafkaFailure() {
        // today=수(04/22): KOSPI, KOSDAQ 각 2건 → 총 4건 시도
        LocalDate today = LocalDate.of(2026, 4, 22);
        when(cursorQueryService.findLastProcessedDate(any()))
                .thenReturn(Optional.of(LocalDate.of(2026, 4, 19)));
        when(kafkaTemplate.send(eq(TOPIC), any(String.class), any(DailyMarketDataQuery.class)))
                .thenThrow(new RuntimeException("Kafka error"))
                .thenReturn(null);

        processorFor(today, DEFAULT_MARKETS).run();

        verify(kafkaTemplate, times(4)).send(eq(TOPIC), any(), any(DailyMarketDataQuery.class));
    }
}
