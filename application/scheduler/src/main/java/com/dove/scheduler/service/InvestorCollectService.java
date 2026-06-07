package com.dove.scheduler.service;

import com.dove.concurrent.Parallel;
import com.dove.concurrent.ParallelException;
import com.dove.investorflow.application.service.InvestorDailyService;
import com.dove.investorflow.domain.entity.InvestorDaily;
import com.dove.jobstatus.JobStatusRegistry;
import com.dove.jobstatus.SchedulerJobName;
import com.dove.stock.application.service.StockQueryService;
import com.dove.stock.domain.entity.Stock;
import com.dove.stock.domain.enums.StockExchange;
import com.dove.stockcollection.application.port.InvestorDailyRow;
import com.dove.stockcollection.application.port.InvestorFetcher;
import com.dove.systemevent.application.service.SystemEventService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * KIS 투자자매매동향을 전 종목 당일 수집해 INVESTOR_DAILY에 upsert한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InvestorCollectService {

    @Value("${collection.concurrency:40}")
    private int concurrency;

    private final InvestorFetcher fetcher;
    private final StockQueryService stockQueryService;
    private final InvestorDailyService investorDailyService;
    private final SystemEventService systemEventService;
    private final JobStatusRegistry jobStatusRegistry;

    /**
     * 전 종목의 당일 투자자동향을 수집한다.
     */
    public void collectAll(LocalDate today) {
        List<Stock> stocks = stockQueryService.findAll();
        log.info("투자자동향 수집 시작: {}종목 / {}", stocks.size(), today);
        jobStatusRegistry.start(SchedulerJobName.INVESTOR_FLOW.name(), stocks.size());

        AtomicInteger done = new AtomicInteger();
        try {
            Parallel.run(stocks, concurrency, stock -> {
                String ticker = stock.getTicker();
                StockExchange exchange = StockExchange.fromMarket(stock.getMarket());

                List<InvestorDailyRow> rows = fetcher.fetch(ticker, today, today);
                if (rows.isEmpty()) return;

                investorDailyService.saveAll(rows.stream()
                        .map(r -> new InvestorDaily(exchange, ticker, r.tradeDate(),
                                r.individualBuy(), r.individualSell(),
                                r.institutionBuy(), r.institutionSell(),
                                r.foreignBuy(), r.foreignSell()))
                        .toList());
                int c = done.incrementAndGet();
                if (c % 100 == 0) jobStatusRegistry.progress(SchedulerJobName.INVESTOR_FLOW.name(), c);
            });
            jobStatusRegistry.complete(SchedulerJobName.INVESTOR_FLOW.name());
            log.info("투자자동향 수집 완료");
        } catch (ParallelException e) {
            Throwable cause = e.getCause();
            log.error("투자자동향 수집 중단 — KIS 오류: {}", cause.getMessage(), cause);
            systemEventService.recordKisApiFailure("INVESTOR", cause.getMessage());
            jobStatusRegistry.fail(SchedulerJobName.INVESTOR_FLOW.name(), cause.getMessage());
            throw e;
        }
    }
}
