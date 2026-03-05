package com.dove.stockconsumer.listener;

import com.dove.stockconsumer.TestConsumerConfiguration;
import com.dove.stockdata.domain.entity.StockData;
import com.dove.stockdata.domain.enums.MarketType;
import com.dove.stockdata.domain.repository.StockDataRepository;
import com.dove.technicalindicator.domain.entity.TechnicalIndicator;
import com.dove.technicalindicator.domain.repository.TechnicalIndicatorRepository;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@SpringBootTest
@Import(TestConsumerConfiguration.class)
@DisplayName("StockIndicatorCalcEventListener 통합 테스트")
class StockIndicatorCalcEventListenerIntegrationTest {

    @Autowired
    private StockIndicatorCalcEventListener stockIndicatorCalcEventListener;

    @Autowired
    private StockDataRepository stockDataRepository;

    @Autowired
    private TechnicalIndicatorRepository technicalIndicatorRepository;

    @Test
    @Transactional
    @DisplayName("기술적 지표 계산 이벤트 수신 시 findTradeDatesFrom으로 조회한 날짜들에 대해 순차 계산한다")
    void shouldCalculateIndicatorsForDateRange() {
        // Given - 5일치 StockData를 미리 저장
        String stockCode = "005930";
        MarketType marketType = MarketType.KOSPI;
        for (int i = 1; i <= 5; i++) {
            stockDataRepository.save(new StockData(
                    marketType, stockCode, LocalDate.of(2024, 1, i),
                    1000L, 100L, (long) (100 + i), 90L, 110L));
        }

        String json = "{\"marketType\":\"KOSPI\",\"stockCode\":\"005930\",\"tradeDate\":\"20240103\"}";
        ConsumerRecord<String, String> record = new ConsumerRecord<>("TECHNICAL_INDICATOR_CALC", 0, 0, stockCode, json);
        Acknowledgment ack = mock(Acknowledgment.class);

        // When
        stockIndicatorCalcEventListener.onIndicatorCalcEventRequest(record, ack);

        // Then - 1/3~1/5 (3일) 계산 대상, SMA_5는 데이터 부족으로 건너뛰지만 다른 지표는 계산됨
        List<TechnicalIndicator> indicators = technicalIndicatorRepository.findAll();
        assertThat(indicators).isNotEmpty();
        assertThat(indicators).allSatisfy(indicator -> {
            assertThat(indicator.getId().getStockCode()).isEqualTo(stockCode);
            assertThat(indicator.getId().getTradeDate()).isAfterOrEqualTo(LocalDate.of(2024, 1, 3));
        });

        verify(ack).acknowledge();
    }
}
