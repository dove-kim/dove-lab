package com.dove.stockbatch.producer;

import com.dove.stockdata.application.dto.IndicatorCalcEventRequest;
import com.dove.stockdata.domain.entity.StockDataChange;
import com.dove.stockdata.domain.enums.MarketType;
import com.dove.stockdata.domain.repository.StockDataChangeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

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
 * <p>10분마다 실행되며, 생성 후 최소 1시간이 경과한 변경만 처리한다.
 * 동일 종목의 여러 변경은 min date로 그룹핑하여 1건의 이벤트로 발행하고,
 * stockCode를 파티션 키로 사용하여 같은 종목의 순서를 보장한다.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PendingIndicatorCalcProcessor {
    private final StockDataChangeRepository stockDataChangeRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    private static final String TOPIC = "TECHNICAL_INDICATOR_CALC";
    private static final Duration MIN_AGE = Duration.ofHours(1);

    @Scheduled(cron = "0 */10 * * * ?")
    @Transactional
    public void processPendingCalculations() {
        LocalDateTime threshold = LocalDateTime.now().minus(MIN_AGE);
        List<StockDataChange> changes = stockDataChangeRepository.findAllByCreatedAtBefore(threshold);
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
            log.debug("기술적 지표 계산 요청 발행 - 시장: {}, 종목: {}, 시작일: {}", key.marketType(), key.stockCode(), minDate);
        });

        stockDataChangeRepository.deleteChanges(changes);
        log.info("처리 완료된 변경 {}건 삭제", changes.size());
    }

    private record StockKey(MarketType marketType, String stockCode) {}
}
