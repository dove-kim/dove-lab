package com.dove.stockbatch.producer;

import com.dove.stockbatch.dto.KrxDailyStockDataRequest;
import com.dove.stockdata.enums.MarketType;
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
     * 주중 아침 8시 5분에 작동하는 배치.
     * 한국 거래소에서 아침 8시 이후로 전날 주식 정보를 조회할 수 있다.
     * 이로인해 8시 5분에 메시지 발행 스케쥴러를 작동한다.
     */
    @Scheduled(cron = "0 5 8 * * MON-FRI")
    public void sendDailyStockDataRequest() {
        LocalDate targetDate = LocalDate.now().minusDays(1); // 전날 데이터 요청

        log.info("일일 주식 데이터 요청 시작 - 대상일: {}", targetDate);

        // KOSPI 메시지 발송
        sendStockDataRequest(MarketType.KOSPI, targetDate);

        // KOSDAQ 메시지 발송
        sendStockDataRequest(MarketType.KOSDAQ, targetDate);

        log.info("일일 주식 데이터 요청 완료 - 대상일: {}", targetDate);
    }

    private void sendStockDataRequest(MarketType marketType, LocalDate baseDate) {
        try {
            String messageKey = String.format("%s-%s", marketType.name().toLowerCase(), baseDate);
            KrxDailyStockDataRequest request = new KrxDailyStockDataRequest(marketType, baseDate);

            kafkaTemplate.send(TOPIC_NAME, messageKey, request);
            log.info("카프카 메시지 발송 성공 - 시장: {}, 날짜: {}", marketType, baseDate);

        } catch (Exception e) {
            log.error("카프카 메시지 전송 실패 - 시장: {}, 날짜: {}, 오류: {}",
                    marketType, baseDate, e.getMessage(), e);
        }
    }
}