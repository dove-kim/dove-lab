package com.dove.scheduler.service;

import com.dove.jobstatus.JobStatusRegistry;
import com.dove.jobstatus.SchedulerJobName;
import com.dove.portfolio.application.service.PortfolioFxRateService;
import com.dove.portfolio.application.service.PortfolioQuoteService;
import com.dove.portfolio.application.service.PortfolioTransactionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.function.IntSupplier;

/**
 * 포트폴리오 시세·환율 일배치 조합 — 가장 오래된 거래일을 기준점으로 환율과 해외 종가 이력의 빈 구간을 채운다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PortfolioMarketDataService {

    private final PortfolioFxRateService fxRateService;
    private final PortfolioQuoteService quoteService;
    private final PortfolioTransactionService transactionService;
    private final JobStatusRegistry jobStatusRegistry;

    /**
     * 환율 이력과 해외 종가를 순차 갱신한다.
     */
    public void refresh() {
        LocalDate start = transactionService.oldestTradeDate().orElseGet(LocalDate::now);
        runStage(SchedulerJobName.PORTFOLIO_FX, () -> fxRateService.refreshFrom(start));
        runStage(SchedulerJobName.PORTFOLIO_QUOTE, () -> quoteService.refreshFrom(start));
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
