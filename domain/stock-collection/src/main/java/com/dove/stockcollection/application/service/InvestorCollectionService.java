package com.dove.stockcollection.application.service;

import com.dove.concurrent.Parallel;
import com.dove.investorflow.application.service.InvestorDailyService;
import com.dove.investorflow.domain.entity.InvestorDaily;
import com.dove.market.domain.enums.MarketType;
import com.dove.stock.application.service.StockQueryService;
import com.dove.stockcollection.application.port.InvestorDailyRow;
import com.dove.stockcollection.application.port.InvestorFetcher;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 투자자매매동향 기간 수집 코어. API 재조회에서 사용한다.
 */
@Slf4j
@Service
@ConditionalOnBean(InvestorFetcher.class)
@RequiredArgsConstructor
public class InvestorCollectionService {

    @Value("${collection.concurrency:40}")
    private int concurrency;

    private final InvestorFetcher fetcher;
    private final StockQueryService stockQueryService;
    private final InvestorDailyService investorDailyService;

    /**
     * 전 종목의 from~to 기간 투자자매매동향을 수집한다.
     */
    public void collect(LocalDate from, LocalDate to, CollectionProgress progress) {
        // FHKST01010900은 KOSPI·KOSDAQ만 제공 — KONEX 제외
        List<String> tickers = stockQueryService.findTickersByMarkets(
                List.of(MarketType.KOSPI, MarketType.KOSDAQ));
        log.info("투자자동향 재조회 시작: {}종목 / {}~{}", tickers.size(), from, to);
        progress.onTotal(tickers.size());

        AtomicInteger done = new AtomicInteger();
        Parallel.run(tickers, concurrency, ticker -> {
            List<InvestorDailyRow> rows = fetcher.fetch(ticker, from, to);
            if (!rows.isEmpty()) {
                investorDailyService.saveAll(toEntities(ticker, rows));
            }
            progress.onProgress(done.incrementAndGet());
        });
    }

    private List<InvestorDaily> toEntities(String ticker, List<InvestorDailyRow> rows) {
        return rows.stream()
                .map(r -> new InvestorDaily(
                        ticker, r.tradeDate(),
                        r.individualBuy(), r.individualSell(),
                        r.institutionBuy(), r.institutionSell(),
                        r.foreignBuy(), r.foreignSell()))
                .toList();
    }
}
