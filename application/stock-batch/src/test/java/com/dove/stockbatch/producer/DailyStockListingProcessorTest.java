package com.dove.stockbatch.producer;

import com.dove.stock.application.dto.DailyStockListingQuery;
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

class DailyStockListingProcessorTest {

    private static final String TOPIC = DailyStockListingQuery.TOPIC;
    private static final List<MarketType> DEFAULT_MARKETS = List.of(MarketType.KOSPI, MarketType.KOSDAQ);

    @SuppressWarnings("unchecked")
    private final KafkaTemplate<String, Object> kafkaTemplate = mock(KafkaTemplate.class);

    private DailyStockListingProcessor processorFor(LocalDate pretendToday, List<MarketType> markets) {
        Clock clock = Clock.fixed(pretendToday.atStartOfDay(ZoneOffset.UTC).toInstant(), ZoneId.of("UTC"));
        return new DailyStockListingProcessor(kafkaTemplate, clock, markets);
    }

    @Test
    @DisplayName("요일 무관 today-1로 설정된 각 시장에 LISTING 이벤트 발행")
    void shouldPublishYesterdayPerMarket() {
        List<MarketType> markets = DEFAULT_MARKETS;
        processorFor(LocalDate.of(2026, 4, 21), markets).run();

        ArgumentCaptor<DailyStockListingQuery> captor = ArgumentCaptor.forClass(DailyStockListingQuery.class);
        verify(kafkaTemplate, times(markets.size())).send(eq(TOPIC), any(), captor.capture());
        assertThat(captor.getAllValues())
                .extracting(DailyStockListingQuery::getBaseDate)
                .containsOnly(LocalDate.of(2026, 4, 20));
        assertThat(captor.getAllValues())
                .extracting(DailyStockListingQuery::getMarketType)
                .containsExactlyInAnyOrderElementsOf(markets);
    }

    @Test
    @DisplayName("KONEX 포함 설정 시 KOSPI/KOSDAQ/KONEX 각각에 발행")
    void shouldPublishKonexWhenConfigured() {
        List<MarketType> markets = List.of(MarketType.KOSPI, MarketType.KOSDAQ, MarketType.KONEX);
        processorFor(LocalDate.of(2026, 4, 21), markets).run();

        ArgumentCaptor<DailyStockListingQuery> captor = ArgumentCaptor.forClass(DailyStockListingQuery.class);
        verify(kafkaTemplate, times(markets.size())).send(eq(TOPIC), any(), captor.capture());
        assertThat(captor.getAllValues())
                .extracting(DailyStockListingQuery::getMarketType)
                .containsExactlyInAnyOrderElementsOf(markets);
    }

    @Test
    @DisplayName("한 시장 전송 실패해도 나머지 시장은 계속 전송")
    void shouldContinueOnFailure() {
        when(kafkaTemplate.send(eq(TOPIC), any(String.class), any(DailyStockListingQuery.class)))
                .thenThrow(new RuntimeException("Kafka error"))
                .thenReturn(null);

        List<MarketType> markets = DEFAULT_MARKETS;
        processorFor(LocalDate.of(2026, 4, 21), markets).run();

        verify(kafkaTemplate, times(markets.size())).send(eq(TOPIC), any(), any(DailyStockListingQuery.class));
    }
}
