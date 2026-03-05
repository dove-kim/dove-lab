package com.dove.stockconsumer.listener;

import com.dove.krxmarketdata.infrastructure.client.KrxStockClient;
import com.dove.krxmarketdata.infrastructure.client.KrxStockResponse;
import com.dove.stockconsumer.TestConsumerConfiguration;
import com.dove.stockdata.domain.entity.StockDataChange;
import com.dove.stockdata.domain.repository.StockDataChangeRepository;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest
@Import(TestConsumerConfiguration.class)
@DisplayName("KrxStockDailyDataEventListener 통합 테스트")
class KrxStockDailyDataEventListenerIntegrationTest {

    @Autowired
    private KrxStockDailyDataEventListener krxStockDailyDataEventListener;

    @MockitoBean
    private KrxStockClient krxStockClient;

    @Autowired
    private StockDataChangeRepository stockDataChangeRepository;

    @Test
    @Transactional
    @DisplayName("KRX 주가 저장 후 StockDataChange가 DB에 기록된다")
    void shouldSaveStockDataAndRecordChange() {
        // Given
        LocalDate tradeDate = LocalDate.of(2024, 1, 15);
        KrxStockResponse response = new KrxStockResponse(List.of(
                createKrxData("20240115", "005930", "삼성전자", 1000L, 70000L, 71000L, 69000L, 72000L),
                createKrxData("20240115", "000660", "SK하이닉스", 500L, 130000L, 131000L, 129000L, 132000L)
        ));
        when(krxStockClient.getDailyKospiStockInfo(any(), eq(tradeDate))).thenReturn(response);

        String json = "{\"baseDate\":\"20240115\",\"marketType\":\"KOSPI\"}";
        ConsumerRecord<String, String> record = new ConsumerRecord<>("KRX_STOCK_PRICE_QUERY", 0, 0, "20240115", json);
        Acknowledgment ack = mock(Acknowledgment.class);

        // When
        krxStockDailyDataEventListener.krxStockDailyData(record, ack);

        // Then
        List<StockDataChange> changes = stockDataChangeRepository.findAll();
        assertThat(changes).hasSize(2);
        assertThat(changes).extracting(c -> c.getId().getStockCode())
                .containsExactlyInAnyOrder("005930", "000660");

        verify(ack).acknowledge();
    }

    @Test
    @Transactional
    @DisplayName("휴장일(빈 응답)이면 StockDataChange가 기록되지 않는다")
    void shouldNotRecordChangeOnHoliday() {
        // Given
        LocalDate tradeDate = LocalDate.of(2024, 1, 14);
        when(krxStockClient.getDailyKospiStockInfo(any(), eq(tradeDate)))
                .thenReturn(new KrxStockResponse(List.of()));

        String json = "{\"baseDate\":\"20240114\",\"marketType\":\"KOSPI\"}";
        ConsumerRecord<String, String> record = new ConsumerRecord<>("KRX_STOCK_PRICE_QUERY", 0, 0, "20240114", json);
        Acknowledgment ack = mock(Acknowledgment.class);

        // When
        krxStockDailyDataEventListener.krxStockDailyData(record, ack);

        // Then
        List<StockDataChange> changes = stockDataChangeRepository.findAll();
        assertThat(changes).isEmpty();

        verify(ack).acknowledge();
    }

    private KrxStockResponse.Data createKrxData(String baseDate, String stockCode, String stockName,
                                                  Long volume, Long openPrice, Long closePrice,
                                                  Long lowPrice, Long highPrice) {
        return new KrxStockResponse.Data(
                baseDate, stockCode, stockName, "KOSPI", "",
                closePrice.toString(), "0", "0.0",
                openPrice.toString(), highPrice.toString(), lowPrice.toString(),
                volume.toString(), "0", "0", "0"
        );
    }
}
