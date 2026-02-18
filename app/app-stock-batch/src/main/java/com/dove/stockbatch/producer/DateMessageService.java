package com.dove.stockbatch.producer;

import com.dove.stockbatch.dto.KrxDailyStockDataRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Slf4j
@Component
@RequiredArgsConstructor
public class DateMessageService {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private static final String TOPIC_NAME = "KRX_DATA_REQUEST";

    /**
     * 매일 22시에 작동하는 배치.
     * 전일(D-1) 주식 데이터를 조회하기 위해 메시지를 발행한다.
     */
    @Scheduled(cron = "0 0 22 * * ?")
    public void sendDailyStockDataRequest() {
        LocalDate targetDate = LocalDate.now().minusDays(1);

        log.info("일일 주식 데이터 요청 시작 - 대상일: {}", targetDate);

        try {
            String messageKey = String.format("daily-%s", targetDate);
            KrxDailyStockDataRequest request = new KrxDailyStockDataRequest(targetDate);

            kafkaTemplate.send(TOPIC_NAME, messageKey, request);
            log.info("카프카 메시지 발송 성공 - 날짜: {}", targetDate);

        } catch (Exception e) {
            log.error("카프카 메시지 전송 실패 - 날짜: {}, 오류: {}",
                    targetDate, e.getMessage(), e);
        }

        log.info("일일 주식 데이터 요청 완료 - 대상일: {}", targetDate);
    }
}
