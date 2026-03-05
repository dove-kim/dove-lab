package com.dove.stockbatch.producer;

import com.dove.stockdata.application.dto.KrxDailyStockDataRequest;
import com.dove.stockdata.domain.enums.MarketType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@SpringBootTest
@DisplayName("DailyStockDataProcessor 통합 테스트")
class DailyStockDataProcessorIntegrationTest {

    @Autowired
    private DailyStockDataProcessor processor;

    @SuppressWarnings("unchecked")
    @MockitoBean
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Test
    @DisplayName("KOSPI와 KOSDAQ 시장별로 KRX_STOCK_PRICE_QUERY 토픽에 메시지를 발행한다")
    void shouldPublishMessagesForEachMarket() {
        // When
        processor.sendDailyStockDataRequest();

        // Then
        ArgumentCaptor<KrxDailyStockDataRequest> captor =
                ArgumentCaptor.forClass(KrxDailyStockDataRequest.class);
        verify(kafkaTemplate, times(2))
                .send(eq("KRX_STOCK_PRICE_QUERY"), any(String.class), captor.capture());

        List<KrxDailyStockDataRequest> requests = captor.getAllValues();
        assertThat(requests).extracting(KrxDailyStockDataRequest::getMarketType)
                .containsExactlyInAnyOrder(MarketType.KOSPI, MarketType.KOSDAQ);
    }

    @Test
    @DisplayName("전일(D-1) 날짜를 기준으로 메시지를 발행한다")
    void shouldPublishWithYesterdayDate() {
        // When
        processor.sendDailyStockDataRequest();

        // Then
        ArgumentCaptor<KrxDailyStockDataRequest> captor =
                ArgumentCaptor.forClass(KrxDailyStockDataRequest.class);
        verify(kafkaTemplate, times(2))
                .send(any(), any(), captor.capture());

        LocalDate expectedDate = LocalDate.now().minusDays(1);
        captor.getAllValues().forEach(request ->
                assertThat(request.getBaseDate()).isEqualTo(expectedDate));
    }

    @Test
    @DisplayName("메시지 키에 시장 유형과 날짜가 포함된다")
    void shouldIncludeMarketTypeAndDateInMessageKey() {
        // When
        processor.sendDailyStockDataRequest();

        // Then
        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        verify(kafkaTemplate, times(2))
                .send(eq("KRX_STOCK_PRICE_QUERY"), keyCaptor.capture(), any(KrxDailyStockDataRequest.class));

        LocalDate expectedDate = LocalDate.now().minusDays(1);
        assertThat(keyCaptor.getAllValues()).containsExactlyInAnyOrder(
                String.format("daily-KOSPI-%s", expectedDate),
                String.format("daily-KOSDAQ-%s", expectedDate)
        );
    }
}
