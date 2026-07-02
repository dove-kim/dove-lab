package com.dove.scheduler.service;

import com.dove.indicator.application.service.RankCalculationService;
import com.dove.jobstatus.JobStatusRegistry;
import com.dove.jobstatus.SchedulerJobName;
import com.dove.stock.domain.enums.MarketUniverse;
import com.dove.stock.domain.enums.PriceType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

/**
 * 전 (universe × 가격유형)의 횡단면 순위를 커서 기준으로 계산하는 스케줄러 단계 서비스.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RankComputeService {

    private final RankCalculationService rankCalculationService;
    private final JobStatusRegistry jobStatusRegistry;

    /**
     * 전 (universe × 가격유형)의 순위를 계산한다. 상한(프런티어)은 universe별 지표 완비일로
     * 자동 결정되므로 {@code today}는 호출 트리거 표시 용도로만 받는다.
     */
    public void calculateAll(LocalDate today) {
        int total = MarketUniverse.values().length * PriceType.values().length;
        jobStatusRegistry.start(SchedulerJobName.RANK.name(), total);
        log.info("횡단면 순위 계산 시작: {} universe (트리거일 {})", total, today);

        int done = 0;
        for (MarketUniverse universe : MarketUniverse.values()) {
            for (PriceType priceType : PriceType.values()) {
                try {
                    rankCalculationService.calculateUniverse(universe, priceType);
                } catch (Exception e) {
                    log.warn("[{}][{}] 순위 계산 실패: {}", universe, priceType, e.getMessage());
                }
                jobStatusRegistry.progress(SchedulerJobName.RANK.name(), ++done);
            }
        }

        jobStatusRegistry.complete(SchedulerJobName.RANK.name());
        log.info("횡단면 순위 계산 완료");
    }
}
