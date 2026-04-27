package com.dove.stockconsumer.listener;

import com.dove.krx.infrastructure.client.KrxStockClient;
import com.dove.krx.infrastructure.client.KrxDailyPriceResponse;
import com.dove.stockconsumer.TestConsumerConfiguration;
import com.dove.stockprice.application.dto.DailyStockPriceQuery;
import com.dove.stockprice.domain.entity.StockDataChange;
import com.dove.stockprice.domain.repository.StockDataChangeRepository;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest
@Import(TestConsumerConfiguration.class)
@TestPropertySource(properties = "distributed-lock.enabled=false")
@DisplayName("DailyStockPriceListener 통합 테스트")
class DailyStockPriceListenerIntegrationTest {

    @Autowired
    private DailyStockPriceListener listener;

    @MockitoBean
    private KrxStockClient krxStockClient;

    @MockitoBean
    private RedissonClient redissonClient;

    @MockitoBean
    private Clock clock;

    @Autowired
    private StockDataChangeRepository stockDataChangeRepository;

    @Test
    @Transactional
    @DisplayName("KRX 데이터가 있으면 StockDataChange가 DB에 기록된다")
    void shouldSaveDailyStockPriceAndRecordChange() {
        LocalDate tradeDate = LocalDate.of(2024, 1, 15);
        // 확정 시점 지나도록 clock 고정 (2024-01-17 10:00 KST)
        when(clock.instant()).thenReturn(LocalDate.of(2024, 1, 17).atTime(10, 0)
                .atZone(ZoneId.of("Asia/Seoul")).toInstant());
        when(clock.getZone()).thenReturn(ZoneId.of("Asia/Seoul"));

        KrxDailyPriceResponse response = new KrxDailyPriceResponse(List.of(
                createKrxData("20240115", "005930", "삼성전자", 1000L, 70000L, 71000L, 69000L, 72000L),
                createKrxData("20240115", "000660", "SK하이닉스", 500L, 130000L, 131000L, 129000L, 132000L)
        ));
        when(krxStockClient.getDailyKospiStockInfo(any(), eq(tradeDate))).thenReturn(response);

        String json = "{\"baseDate\":\"20240115\",\"marketType\":\"KOSPI\"}";
        ConsumerRecord<String, String> record = new ConsumerRecord<>(
                DailyStockPriceQuery.TOPIC, 0, 0, "20240115", json);
        Acknowledgment ack = mock(Acknowledgment.class);

        listener.onDailyStockPriceQuery(record, ack);

        List<StockDataChange> changes = stockDataChangeRepository.findAll();
        assertThat(changes).hasSize(2);
        assertThat(changes).extracting(c -> c.getId().getStockCode())
                .containsExactlyInAnyOrder("005930", "000660");

        verify(ack).acknowledge();
    }

    @Test
    @Transactional
    @DisplayName("확정 시점 이후 빈 응답(Holiday) → StockDataChange 없음")
    void shouldNotRecordChangeOnHoliday() {
        LocalDate tradeDate = LocalDate.of(2024, 1, 14);
        when(clock.instant()).thenReturn(LocalDate.of(2024, 1, 17).atTime(10, 0)
                .atZone(ZoneId.of("Asia/Seoul")).toInstant());
        when(clock.getZone()).thenReturn(ZoneId.of("Asia/Seoul"));
        when(krxStockClient.getDailyKospiStockInfo(any(), eq(tradeDate)))
                .thenReturn(new KrxDailyPriceResponse(List.of()));

        String json = "{\"baseDate\":\"20240114\",\"marketType\":\"KOSPI\"}";
        ConsumerRecord<String, String> record = new ConsumerRecord<>(
                DailyStockPriceQuery.TOPIC, 0, 0, "20240114", json);
        Acknowledgment ack = mock(Acknowledgment.class);

        listener.onDailyStockPriceQuery(record, ack);

        List<StockDataChange> changes = stockDataChangeRepository.findAll();
        assertThat(changes).isEmpty();

        verify(ack).acknowledge();
    }

    private KrxDailyPriceResponse.Data createKrxData(String baseDate, String stockCode, String stockName,
                                                Long volume, Long openPrice, Long closePrice,
                                                Long lowPrice, Long highPrice) {
        return new KrxDailyPriceResponse.Data(
                baseDate, stockCode, stockName, "KOSPI", "",
                closePrice.toString(), "0", "0.0",
                openPrice.toString(), highPrice.toString(), lowPrice.toString(),
                volume.toString(), "0", "0", "0"
        );
    }
}
