package com.dove.stockbatch.producer;

import com.dove.krx.KrxDataPolicy;
import com.dove.market.application.dto.DailyMarketDataQuery;
import com.dove.market.application.service.MarketDataCursorQueryService;
import com.dove.market.domain.enums.MarketType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

/** 08:05 KST에 MarketDataCursor 다음 날부터 lastAvailableDate까지 DAILY_MARKET_DATA_QUERY 이벤트 발행. */
@Slf4j
@Component
public class DailyMarketDataProcessor {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final MarketDataCursorQueryService cursorQueryService;
    private final Clock clock;
    private final List<MarketType> targetMarkets;
    private final LocalDate initialDate;

    public DailyMarketDataProcessor(
            KafkaTemplate<String, Object> kafkaTemplate,
            MarketDataCursorQueryService cursorQueryService,
            Clock clock,
            @Value("${krx.target-markets:KOSPI,KOSDAQ}") List<MarketType> targetMarkets,
            @Value("${market.data.initial-date:2010-01-01}") LocalDate initialDate) {
        this.kafkaTemplate = kafkaTemplate;
        this.cursorQueryService = cursorQueryService;
        this.clock = clock;
        this.targetMarkets = targetMarkets;
        this.initialDate = initialDate;
    }

    @Scheduled(cron = "0 5 8 * * *", zone = "Asia/Seoul")
    public void run() {
        LocalDate upTo = KrxDataPolicy.lastAvailableDate(LocalDate.now(clock));
        for (MarketType market : targetMarkets) {
            LocalDate from = cursorQueryService.findLastProcessedDate(market)
                    .map(d -> d.plusDays(1))
                    .orElse(initialDate);

            if (from.isAfter(upTo)) {
                continue;
            }

            from.datesUntil(upTo.plusDays(1))
                    .forEach(date -> publish(market, date));
        }
    }

    private void publish(MarketType market, LocalDate date) {
        try {
            DailyMarketDataQuery query = new DailyMarketDataQuery(market, date);
            kafkaTemplate.send(DailyMarketDataQuery.TOPIC, query.partitionKey(), query);
        } catch (Exception e) {
            log.error("Kafka 메시지 전송 실패: market={}, date={}, error={}", market, date, e.getMessage(), e);
        }
    }
}
