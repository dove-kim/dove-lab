package com.dove.stockbatch.producer;

import com.dove.stockbatch.dto.KrxDailyStockDataRequest;
import com.dove.stockdata.enums.MarketType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Slf4j
@Component
@RequiredArgsConstructor
public class DateMessageService {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private static final String TOPIC_NAME = "KRX_DATA_REQUEST";
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Scheduled(fixedRate = 60000) // 1분마다 실행 (60초 = 60000ms)
    public void sendCurrentDate() {
        try {
            kafkaTemplate.send(TOPIC_NAME, "date-key",
                    new KrxDailyStockDataRequest(MarketType.KOSDAQ, LocalDate.now().minusDays(2))
            );
        } catch (Exception e) {
            log.error("카프카 메시지 전송 실패: {}", e.getMessage(), e);
        }
    }

}
