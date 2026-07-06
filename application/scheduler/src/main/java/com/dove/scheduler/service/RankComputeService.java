package com.dove.scheduler.service;

import com.dove.concurrent.Parallel;
import com.dove.indicator.application.service.RankCalculationService;
import com.dove.jobstatus.JobStatusRegistry;
import com.dove.jobstatus.SchedulerJobName;
import com.dove.scheduler.dto.RankUnit;
import com.dove.stock.domain.enums.MarketUniverse;
import com.dove.stock.domain.enums.PriceType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 전 (universe × 가격유형)의 횡단면 순위를 커서 기준으로 계산하는 스케줄러 단계 서비스.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RankComputeService {

    @Value("${rank.concurrency:3}")
    private int concurrency;

    private final RankCalculationService rankCalculationService;
    private final JobStatusRegistry jobStatusRegistry;

    /**
     * 전 (universe × 가격유형)의 순위를 병렬 계산한다. 상한(프런티어)은 universe별 지표 완비일로
     * 자동 결정되므로 {@code today}는 호출 트리거 표시 용도로만 받는다.
     */
    public void calculateAll(LocalDate today) {
        List<RankUnit> units = new ArrayList<>();
        for (MarketUniverse universe : MarketUniverse.values()) {
            for (PriceType priceType : PriceType.values()) {
                units.add(new RankUnit(universe, priceType));
            }
        }
        jobStatusRegistry.start(SchedulerJobName.RANK.name(), units.size());
        log.info("횡단면 순위 계산 시작: {} 유닛 / 동시 {} (트리거일 {})", units.size(), concurrency, today);

        AtomicInteger done = new AtomicInteger();
        // 유닛별로 예외를 직접 삼키므로(로그) Parallel의 fail-fast는 발생하지 않는다 (best-effort).
        Parallel.run(units, concurrency, unit -> {
            try {
                rankCalculationService.calculateUniverse(unit.universe(), unit.priceType());
            } catch (Exception e) {
                log.warn("[{}][{}] 순위 계산 실패: {}", unit.universe(), unit.priceType(), e.getMessage());
            } finally {
                jobStatusRegistry.progress(SchedulerJobName.RANK.name(), done.incrementAndGet());
            }
        });

        jobStatusRegistry.complete(SchedulerJobName.RANK.name());
        log.info("횡단면 순위 계산 완료");
    }
}
