package com.dove.stockconsumer.listener;

import com.dove.stockconsumer.dto.KrxDailyStockDataRequest;
import com.dove.stockkrxdata.serivce.KrxStockDailySaveService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class KrxStockDailyDataEventListener {
    private final ObjectMapper objectMapper;

    private final KrxStockDailySaveService krxStockDailySaveService;

    @KafkaListener(
            groupId = "krxDailyData-1",
            topics = "KRX_DATA_REQUEST",
            concurrency = "3"
    )
    public void krxStockDailyData(ConsumerRecord<String, String> data, Acknowledgment acknowledgment) {
        String messageValue = data.value(); // 메시지 value를 String으로 받음
        KrxDailyStockDataRequest krxDailyStockDataRequest = null;

        try {
            // String 메시지를 KrxDailyStockDataRequest DTO로 수동 파싱
            krxDailyStockDataRequest = objectMapper.readValue(messageValue, KrxDailyStockDataRequest.class);

            // 한국 거래소 데이터 조회 및 저장 요청
            krxStockDailySaveService
                    .saveKrxDailyStockData(krxDailyStockDataRequest.getMarketType(), krxDailyStockDataRequest.getBaseDate());

        } catch (Exception e) {
            log.error(e.toString(), e);
        }
        acknowledgment.acknowledge();
    }
}
