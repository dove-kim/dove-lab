package com.dove.stockbatch.producer;

import com.dove.technicalindicator.application.dto.IndicatorCalcEventRequest;
import com.dove.stockprice.application.service.StockDataChangeCommandService;
import com.dove.stockprice.application.service.StockDataChangeQueryService;
import com.dove.stockprice.domain.entity.StockDataChange;
import com.dove.market.domain.enums.MarketType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PendingIndicatorCalcProcessorTest {

    @Mock
    private StockDataChangeQueryService stockDataChangeQueryService;

    @Mock
    private StockDataChangeCommandService stockDataChangeCommandService;

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @InjectMocks
    private PendingIndicatorCalcProcessor processor;

    @Test
    @DisplayName("처리 대상이 없으면 이벤트를 발행하지 않는다")
    void shouldNotPublishWhenNoPending() {
        when(stockDataChangeQueryService.findChangesOlderThan(any())).thenReturn(List.of());

        processor.processPendingCalculations();

        verify(kafkaTemplate, never()).send(any(), any(), any());
        verify(stockDataChangeCommandService, never()).deleteAll(any());
    }

    @Test
    @DisplayName("1시간 미만인 변경은 조회되지 않는다 (threshold 검증)")
    void shouldNotPublishWhenChangesAreTooRecent() {
        when(stockDataChangeQueryService.findChangesOlderThan(any())).thenReturn(List.of());

        processor.processPendingCalculations();

        ArgumentCaptor<LocalDateTime> thresholdCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(stockDataChangeQueryService).findChangesOlderThan(thresholdCaptor.capture());

        LocalDateTime threshold = thresholdCaptor.getValue();
        assertThat(threshold).isBefore(LocalDateTime.now().minusMinutes(59));
    }

    @Test
    @DisplayName("동일 종목의 여러 날짜 변경을 min date로 그룹핑한다")
    void shouldGroupByStockAndFindMinDate() {
        StockDataChange change1 = createChange(MarketType.KOSPI, "005930", LocalDate.of(2024, 1, 15));
        StockDataChange change2 = createChange(MarketType.KOSPI, "005930", LocalDate.of(2024, 1, 10));
        when(stockDataChangeQueryService.findChangesOlderThan(any())).thenReturn(List.of(change1, change2));

        processor.processPendingCalculations();

        ArgumentCaptor<IndicatorCalcEventRequest> requestCaptor =
                ArgumentCaptor.forClass(IndicatorCalcEventRequest.class);
        verify(kafkaTemplate, times(1)).send(any(), any(), requestCaptor.capture());

        IndicatorCalcEventRequest request = requestCaptor.getValue();
        assertThat(request.getStockCode()).isEqualTo("005930");
        assertThat(request.getTradeDate()).isEqualTo(LocalDate.of(2024, 1, 10));
    }

    @Test
    @DisplayName("종목별로 이벤트를 발행하며 stockCode를 파티션 키로 사용한다")
    void shouldPublishEventPerStockWithStockCodeAsKey() {
        StockDataChange change1 = createChange(MarketType.KOSPI, "005930", LocalDate.of(2024, 1, 15));
        StockDataChange change2 = createChange(MarketType.KOSDAQ, "035720", LocalDate.of(2024, 1, 15));
        when(stockDataChangeQueryService.findChangesOlderThan(any())).thenReturn(List.of(change1, change2));

        processor.processPendingCalculations();

        ArgumentCaptor<String> topicCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<IndicatorCalcEventRequest> requestCaptor =
                ArgumentCaptor.forClass(IndicatorCalcEventRequest.class);
        verify(kafkaTemplate, times(2)).send(topicCaptor.capture(), keyCaptor.capture(), requestCaptor.capture());

        assertThat(topicCaptor.getAllValues()).containsOnly("TECHNICAL_INDICATOR_CALC");
        assertThat(keyCaptor.getAllValues()).containsExactlyInAnyOrder("005930", "035720");
    }

    @Test
    @DisplayName("이벤트 발행 후 처리 완료된 변경을 삭제한다")
    void shouldDeleteAfterPublishing() {
        StockDataChange change = createChange(MarketType.KOSPI, "005930", LocalDate.of(2024, 1, 15));
        List<StockDataChange> changes = List.of(change);
        when(stockDataChangeQueryService.findChangesOlderThan(any())).thenReturn(changes);

        processor.processPendingCalculations();

        verify(stockDataChangeCommandService).deleteAll(changes);
    }

    private StockDataChange createChange(MarketType marketType, String stockCode, LocalDate tradeDate) {
        return new StockDataChange(marketType, stockCode, tradeDate);
    }
}
