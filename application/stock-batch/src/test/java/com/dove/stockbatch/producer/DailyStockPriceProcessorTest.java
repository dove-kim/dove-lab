package com.dove.stockbatch.producer;

import com.dove.stockprice.application.dto.DailyStockPriceQuery;
import com.dove.market.domain.enums.MarketType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DailyStockPriceProcessorTest {

    private static final String TOPIC = DailyStockPriceQuery.TOPIC;
    private static final List<MarketType> DEFAULT_MARKETS = List.of(MarketType.KOSPI, MarketType.KOSDAQ);

    @SuppressWarnings("unchecked")
    private final KafkaTemplate<String, Object> kafkaTemplate = mock(KafkaTemplate.class);

    private DailyStockPriceProcessor processorFor(LocalDate pretendToday, List<MarketType> markets) {
        Clock clock = Clock.fixed(pretendToday.atStartOfDay(ZoneOffset.UTC).toInstant(), ZoneId.of("UTC"));
        return new DailyStockPriceProcessor(kafkaTemplate, clock, markets);
    }

    @Test
    @DisplayName("평일: today-1로 설정된 각 시장에 PRICE 이벤트 발행")
    void shouldPublishYesterdayOnWeekday() {
        List<MarketType> markets = DEFAULT_MARKETS;
        processorFor(LocalDate.of(2026, 4, 21), markets).run();

        ArgumentCaptor<DailyStockPriceQuery> captor = ArgumentCaptor.forClass(DailyStockPriceQuery.class);
        verify(kafkaTemplate, times(markets.size())).send(eq(TOPIC), any(), captor.capture());
        assertThat(captor.getAllValues())
                .extracting(DailyStockPriceQuery::getBaseDate)
                .containsOnly(LocalDate.of(2026, 4, 20));
        assertThat(captor.getAllValues())
                .extracting(DailyStockPriceQuery::getMarketType)
                .containsExactlyInAnyOrderElementsOf(markets);
    }

    @Test
    @DisplayName("월요일이어도 요일 분기 없이 today-1 (일요일) 발행")
    void shouldPublishYesterdayOnMondayWithoutWeekdaySkip() {
        List<MarketType> markets = DEFAULT_MARKETS;
        processorFor(LocalDate.of(2026, 4, 20), markets).run();

        ArgumentCaptor<DailyStockPriceQuery> captor = ArgumentCaptor.forClass(DailyStockPriceQuery.class);
        verify(kafkaTemplate, times(markets.size())).send(eq(TOPIC), any(), captor.capture());
        assertThat(captor.getAllValues())
                .extracting(DailyStockPriceQuery::getBaseDate)
                .containsOnly(LocalDate.of(2026, 4, 19));
    }

    @Test
    @DisplayName("주말 실행도 동일 — Kafka 발행만")
    void shouldPublishOnWeekendWithoutCalendarWrite() {
        List<MarketType> markets = DEFAULT_MARKETS;
        processorFor(LocalDate.of(2026, 4, 25), markets).run();

        ArgumentCaptor<DailyStockPriceQuery> captor = ArgumentCaptor.forClass(DailyStockPriceQuery.class);
        verify(kafkaTemplate, times(markets.size())).send(eq(TOPIC), any(), captor.capture());
        assertThat(captor.getAllValues())
                .extracting(DailyStockPriceQuery::getBaseDate)
                .containsOnly(LocalDate.of(2026, 4, 24));
    }

    @Test
    @DisplayName("KONEX 포함 설정 시 KOSPI/KOSDAQ/KONEX 각각에 발행")
    void shouldPublishKonexWhenConfigured() {
        List<MarketType> markets = List.of(MarketType.KOSPI, MarketType.KOSDAQ, MarketType.KONEX);
        processorFor(LocalDate.of(2026, 4, 21), markets).run();

        ArgumentCaptor<DailyStockPriceQuery> captor = ArgumentCaptor.forClass(DailyStockPriceQuery.class);
        verify(kafkaTemplate, times(markets.size())).send(eq(TOPIC), any(), captor.capture());
        assertThat(captor.getAllValues())
                .extracting(DailyStockPriceQuery::getMarketType)
                .containsExactlyInAnyOrderElementsOf(markets);
    }

    @Test
    @DisplayName("한 시장 전송 실패해도 나머지 시장은 계속 전송")
    void shouldContinueOnFailure() {
        when(kafkaTemplate.send(eq(TOPIC), any(String.class), any(DailyStockPriceQuery.class)))
                .thenThrow(new RuntimeException("Kafka error"))
                .thenReturn(null);

        List<MarketType> markets = DEFAULT_MARKETS;
        processorFor(LocalDate.of(2026, 4, 21), markets).run();

        verify(kafkaTemplate, times(markets.size())).send(eq(TOPIC), any(), any(DailyStockPriceQuery.class));
    }
}
