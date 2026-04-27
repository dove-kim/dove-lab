package com.dove.stockbatch.producer;

import com.dove.market.domain.enums.MarketType;
import com.dove.stockprice.application.service.StockDataChangeCommandService;
import com.dove.stockprice.application.service.StockDataChangeQueryService;
import com.dove.stockprice.domain.entity.StockDataChange;
import com.dove.technicalindicator.application.dto.IndicatorCalcEventRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.reducing;

/**
 * 미처리된 주가 변경(StockDataChange)을 주기적으로 조회하여 기술적 지표 계산 이벤트를 발행한다.
 *
 */
@Component
@RequiredArgsConstructor
public class PendingIndicatorCalcProcessor {
    private final StockDataChangeQueryService stockDataChangeQueryService;
    private final StockDataChangeCommandService stockDataChangeCommandService;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    private static final String TOPIC = "TECHNICAL_INDICATOR_CALC";
    private static final Duration MIN_AGE = Duration.ofHours(1);

    @Scheduled(cron = "0 */10 * * * ?")
    public void processPendingCalculations() {
        LocalDateTime threshold = LocalDateTime.now().minus(MIN_AGE);
        List<StockDataChange> changes = stockDataChangeQueryService.findChangesOlderThan(threshold);
        if (changes.isEmpty()) return;

        Map<StockKey, LocalDate> minDates = changes.stream()
                .collect(groupingBy(
                        c -> new StockKey(c.getId().getMarketType(), c.getId().getStockCode()),
                        reducing(null, c -> c.getId().getTradeDate(),
                                (a, b) -> a == null ? b : a.isBefore(b) ? a : b)
                ));

        minDates.forEach((key, minDate) -> {
            kafkaTemplate.send(TOPIC, key.stockCode(),
                    new IndicatorCalcEventRequest(key.marketType(), key.stockCode(), minDate));
        });

        stockDataChangeCommandService.deleteAll(changes);
    }

    private record StockKey(MarketType marketType, String stockCode) {}
}
