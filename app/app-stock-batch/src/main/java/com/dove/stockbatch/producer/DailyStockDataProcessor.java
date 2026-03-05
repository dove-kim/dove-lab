package com.dove.stockbatch.producer;

import com.dove.stockdata.application.dto.KrxDailyStockDataRequest;
import com.dove.stockdata.domain.enums.MarketType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * 매일 22시에 전일(D-1) 주가 조회 메시지를 시장별로 발행하는 배치 프로세서.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DailyStockDataProcessor {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private static final String TOPIC_NAME = "KRX_STOCK_PRICE_QUERY";
    private static final MarketType[] TARGET_MARKETS = {MarketType.KOSPI, MarketType.KOSDAQ};

    @Scheduled(cron = "0 0 22 * * ?")
    public void sendDailyStockDataRequest() {
        LocalDate targetDate = LocalDate.now().minusDays(1);

        log.info("일일 주식 데이터 요청 시작 - 대상일: {}", targetDate);

        for (MarketType marketType : TARGET_MARKETS) {
            try {
                String messageKey = String.format("daily-%s-%s", marketType, targetDate);
                KrxDailyStockDataRequest request = new KrxDailyStockDataRequest(targetDate, marketType);

                kafkaTemplate.send(TOPIC_NAME, messageKey, request);
                log.debug("카프카 메시지 발송 성공 - 시장: {}, 날짜: {}", marketType, targetDate);

            } catch (Exception e) {
                log.error("카프카 메시지 전송 실패 - 시장: {}, 날짜: {}, 오류: {}",
                        marketType, targetDate, e.getMessage(), e);
            }
        }

        log.info("일일 주식 데이터 요청 완료 - 대상일: {}", targetDate);
    }
}
