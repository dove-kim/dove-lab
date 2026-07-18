package com.dove.scheduler.service;

import com.dove.jobstatus.JobStatusRegistry;
import com.dove.jobstatus.SchedulerJobName;
import com.dove.portfolio.application.service.PortfolioFxRateService;
import com.dove.portfolio.application.service.PortfolioQuoteService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.function.IntSupplier;

/**
 * 포트폴리오 시세·환율 일배치 조합 — 보유 해외 종목 종가와 원통화 환율을 갱신한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PortfolioMarketDataService {

    private final PortfolioFxRateService fxRateService;
    private final PortfolioQuoteService quoteService;
    private final JobStatusRegistry jobStatusRegistry;

    /**
     * 환율과 해외 종가를 순차 갱신한다.
     */
    public void refresh() {
        runStage(SchedulerJobName.PORTFOLIO_FX, fxRateService::refreshAll);
        runStage(SchedulerJobName.PORTFOLIO_QUOTE, quoteService::refreshOverseas);
    }

    private void runStage(SchedulerJobName job, IntSupplier task) {
        jobStatusRegistry.start(job.name(), 0);
        try {
            int updated = task.getAsInt();
            jobStatusRegistry.progress(job.name(), updated);
            jobStatusRegistry.complete(job.name());
        } catch (RuntimeException e) {
            jobStatusRegistry.fail(job.name(), e.getMessage());
            throw e;
        }
    }
}
