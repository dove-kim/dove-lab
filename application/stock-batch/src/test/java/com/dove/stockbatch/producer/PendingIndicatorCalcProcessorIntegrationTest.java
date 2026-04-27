package com.dove.stockbatch.producer;

import com.dove.technicalindicator.application.dto.IndicatorCalcEventRequest;
import com.dove.stockprice.domain.entity.StockDataChange;
import com.dove.market.domain.enums.MarketType;
import com.dove.stockprice.domain.repository.StockDataChangeRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@SpringBootTest
@DisplayName("PendingIndicatorCalcProcessor 통합 테스트")
class PendingIndicatorCalcProcessorIntegrationTest {

    @Autowired
    private PendingIndicatorCalcProcessor processor;

    @Autowired
    private StockDataChangeRepository stockDataChangeRepository;

    @SuppressWarnings("unchecked")
    @MockitoBean
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Test
    @DisplayName("1시간 이상 된 변경만 이벤트로 발행하고, 1시간 미만인 변경은 DB에 남는다")
    void shouldPublishOnlyChangesOlderThanOneHour() {
        // Given
        StockDataChange oldChange = new StockDataChange(MarketType.KOSPI, "005930", LocalDate.of(2024, 1, 10));
        ReflectionTestUtils.setField(oldChange, "createdAt", LocalDateTime.now().minusHours(2));
        stockDataChangeRepository.save(oldChange);

        StockDataChange recentChange = new StockDataChange(MarketType.KOSPI, "000660", LocalDate.of(2024, 1, 10));
        ReflectionTestUtils.setField(recentChange, "createdAt", LocalDateTime.now().minusMinutes(30));
        stockDataChangeRepository.save(recentChange);

        // When
        processor.processPendingCalculations();

        // Then
        verify(kafkaTemplate).send(eq("TECHNICAL_INDICATOR_CALC"), eq("005930"), any(IndicatorCalcEventRequest.class));

        List<StockDataChange> remaining = stockDataChangeRepository.findAll();
        assertThat(remaining).hasSize(1);
        assertThat(remaining.get(0).getId().getStockCode()).isEqualTo("000660");
    }

    @Test
    @DisplayName("동일 종목 여러 날짜 변경은 종목별 min date로 그룹핑하여 이벤트를 발행한다")
    void shouldGroupByStockAndPublishMinDate() {
        // Given
        StockDataChange change1 = new StockDataChange(MarketType.KOSPI, "005930", LocalDate.of(2024, 1, 15));
        ReflectionTestUtils.setField(change1, "createdAt", LocalDateTime.now().minusHours(2));
        stockDataChangeRepository.save(change1);

        StockDataChange change2 = new StockDataChange(MarketType.KOSPI, "005930", LocalDate.of(2024, 1, 10));
        ReflectionTestUtils.setField(change2, "createdAt", LocalDateTime.now().minusHours(2));
        stockDataChangeRepository.save(change2);

        StockDataChange change3 = new StockDataChange(MarketType.KOSPI, "005930", LocalDate.of(2024, 1, 20));
        ReflectionTestUtils.setField(change3, "createdAt", LocalDateTime.now().minusHours(2));
        stockDataChangeRepository.save(change3);

        // When
        processor.processPendingCalculations();

        // Then
        ArgumentCaptor<IndicatorCalcEventRequest> captor = ArgumentCaptor.forClass(IndicatorCalcEventRequest.class);
        verify(kafkaTemplate).send(eq("TECHNICAL_INDICATOR_CALC"), eq("005930"), captor.capture());

        IndicatorCalcEventRequest request = captor.getValue();
        assertThat(request.getTradeDate()).isEqualTo(LocalDate.of(2024, 1, 10));
        assertThat(request.getMarketType()).isEqualTo(MarketType.KOSPI);
    }

    @Test
    @DisplayName("이벤트 발행 시 stockCode가 Kafka 메시지 키로 사용된다")
    void shouldUseStockCodeAsPartitionKey() {
        // Given
        StockDataChange change = new StockDataChange(MarketType.KOSDAQ, "035420", LocalDate.of(2024, 1, 10));
        ReflectionTestUtils.setField(change, "createdAt", LocalDateTime.now().minusHours(2));
        stockDataChangeRepository.save(change);

        // When
        processor.processPendingCalculations();

        // Then
        verify(kafkaTemplate).send(eq("TECHNICAL_INDICATOR_CALC"), eq("035420"), any(IndicatorCalcEventRequest.class));
    }

    @Test
    @DisplayName("발행 완료된 변경만 삭제하고, 미처리 변경은 유지된다")
    void shouldDeleteProcessedChangesOnly() {
        // Given
        StockDataChange processed1 = new StockDataChange(MarketType.KOSPI, "005930", LocalDate.of(2024, 1, 10));
        ReflectionTestUtils.setField(processed1, "createdAt", LocalDateTime.now().minusHours(3));
        stockDataChangeRepository.save(processed1);

        StockDataChange processed2 = new StockDataChange(MarketType.KOSPI, "000660", LocalDate.of(2024, 1, 10));
        ReflectionTestUtils.setField(processed2, "createdAt", LocalDateTime.now().minusHours(2));
        stockDataChangeRepository.save(processed2);

        StockDataChange unprocessed = new StockDataChange(MarketType.KOSPI, "035420", LocalDate.of(2024, 1, 10));
        ReflectionTestUtils.setField(unprocessed, "createdAt", LocalDateTime.now().minusMinutes(10));
        stockDataChangeRepository.save(unprocessed);

        // When
        processor.processPendingCalculations();

        // Then
        List<StockDataChange> remaining = stockDataChangeRepository.findAll();
        assertThat(remaining).hasSize(1);
        assertThat(remaining.get(0).getId().getStockCode()).isEqualTo("035420");
    }

    @Test
    @DisplayName("처리할 변경이 없으면 이벤트를 발행하지 않는다")
    void shouldNotPublishWhenNoPendingChanges() {
        // When
        processor.processPendingCalculations();

        // Then
        verify(kafkaTemplate, never()).send(any(String.class), any(String.class), any());
    }
}
