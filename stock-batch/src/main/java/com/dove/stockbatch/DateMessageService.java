package com.dove.stockbatch;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Slf4j
@Component
@RequiredArgsConstructor
public class DateMessageService {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private static final String TOPIC_NAME = "daily-date-topic";
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Scheduled(fixedRate = 60000) // 1분마다 실행 (60초 = 60000ms)
    public void sendCurrentDate() {
        log.info("hi");
        String currentDate = LocalDateTime.now().format(formatter);
        String message = String.format("현재 날짜와 시간: %s", currentDate);

        try {
            kafkaTemplate.send(TOPIC_NAME, "date-key", message);
        } catch (Exception e) {
            log.error("카프카 메시지 전송 실패: {}", e.getMessage(), e);
        }
    }

}
