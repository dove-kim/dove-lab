package com.dove.scheduler.service;

import com.dove.concurrent.Parallel;
import com.dove.indicator.application.service.IndicatorBulkCalculateService;
import com.dove.jobstatus.JobStatusRegistry;
import com.dove.scheduler.dto.IndicatorGroup;
import com.dove.jobstatus.SchedulerJobName;
import com.dove.stock.application.service.StockQueryService;
import com.dove.stock.domain.enums.PriceType;
import com.dove.stock.domain.enums.StockExchange;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 커서 기반 기술적 지표 계산 서비스.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class IndicatorComputeService {

    @Value("${indicator.concurrency:10}")
    private int concurrency;

    /** 지표 계산 시작일 하한 (비우면 전체 이력). 최초 계산량 제한용 — 예: 2026-01-01. */
    @Value("${indicator.start-date:}")
    private String startDateProp;

    private final StockQueryService stockQueryService;
    private final IndicatorBulkCalculateService bulkCalculateService;
    private final JobStatusRegistry jobStatusRegistry;

    /**
     * 전 (종목 × 거래소 × 가격유형) 그룹의 기술적 지표를 병렬 계산한다.
     */
    public void computeAll(LocalDate today) {
        LocalDate startFloor = (startDateProp == null || startDateProp.isBlank())
                ? null : LocalDate.parse(startDateProp.trim());
        List<String> tickers = stockQueryService.findAllTickers();
        List<IndicatorGroup> groups = buildIndicatorGroups(tickers);
        log.info("기술적 지표 계산 시작: {}종목 / {}그룹 (시작일 하한: {})",
                tickers.size(), groups.size(), startFloor == null ? "전체" : startFloor);
        jobStatusRegistry.start(SchedulerJobName.INDICATOR.name(), groups.size());

        AtomicInteger done = new AtomicInteger();
        // 그룹별로 예외를 직접 삼키므로(로그) Parallel의 fail-fast는 발생하지 않는다 (best-effort).
        Parallel.run(groups, concurrency, group -> {
            try {
                bulkCalculateService.calculateGroup(
                        group.ticker(), group.exchange(), group.priceType(), today, startFloor);
            } catch (Exception e) {
                log.warn("[{}][{}][{}] 지표 계산 실패: {}",
                        group.ticker(), group.exchange(), group.priceType(), e.getMessage());
            } finally {
                int c = done.incrementAndGet();
                if (c % 100 == 0) jobStatusRegistry.progress(SchedulerJobName.INDICATOR.name(), c);
            }
        });

        jobStatusRegistry.complete(SchedulerJobName.INDICATOR.name());
        log.info("기술적 지표 계산 완료");
    }

    /**
     * (종목 × 거래소 × 가격유형) 그룹 목록.
     */
    private List<IndicatorGroup> buildIndicatorGroups(List<String> tickers) {
        List<IndicatorGroup> groups = new ArrayList<>();
        for (String ticker : tickers) {
            for (StockExchange exchange : StockExchange.values()) {
                for (PriceType priceType : PriceType.values()) {
                    groups.add(new IndicatorGroup(ticker, exchange, priceType));
                }
            }
        }
        return groups;
    }
}
