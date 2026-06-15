package com.dove.stockcollection.application.service;

import com.dove.concurrent.Parallel;
import com.dove.stock.application.service.StockCommandService;
import com.dove.stock.application.service.StockQueryService;
import com.dove.stock.application.service.StockTagValueService;
import com.dove.stock.domain.enums.TagField;
import com.dove.stockcollection.application.port.StockDetailFetcher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * KIS API로 전 종목 상세 정보를 수집해 STOCK_DETAIL을 upsert한다.
 */
@Slf4j
@Service
@ConditionalOnBean(StockDetailFetcher.class)
@RequiredArgsConstructor
public class StockDetailCollectionService {

    @Value("${collection.concurrency:40}")
    private int concurrency;

    private final StockDetailFetcher fetcher;
    private final StockQueryService stockQueryService;
    private final StockCommandService stockCommandService;
    private final StockTagValueService tagValueService;

    /**
     * 전 종목의 주식기본·상품기본 정보를 병렬 수집해 upsert한다.
     *
     * @param progress 진행 상황 리스너
     * @throws com.dove.concurrent.ParallelException 병렬 수집 중 KIS 오류 발생 시
     */
    public void updateAll(CollectionProgress progress) {
        List<String> tickers = stockQueryService.findAllTickers();
        progress.onTotal(tickers.size());
        log.info("종목 상세 수집 시작: {}종목", tickers.size());

        AtomicInteger done = new AtomicInteger();
        // (field+value) 조합 중복 등록 방지
        Set<String> seenTags = ConcurrentHashMap.newKeySet();

        int maxFailures = Math.max(20, tickers.size() / 10);
        List<String> failed = Parallel.runResilient(tickers, concurrency, maxFailures, ticker -> {
            applyStockInfo(ticker, seenTags);
            applyProductInfo(ticker, seenTags);
            progress.onProgress(done.incrementAndGet());
        });
        if (!failed.isEmpty()) {
            log.warn("종목 상세 {}종목 중 {}건 실패(건너뜀): {}", tickers.size(), failed.size(),
                    failed.stream().distinct().limit(10).toList());
        }

        log.info("종목 상세 수집 완료");
    }

    private void applyStockInfo(String ticker, Set<String> seenTags) {
        fetcher.fetchStockInfo(ticker).ifPresent(d -> {
            stockCommandService.applyStockInfo(ticker,
                    d.listedShares(), d.capitalAmount(), d.faceValue(),
                    d.stockKindCd(), d.etfDvsnCd(), d.reitsKindCd(), d.kospi200ItemYn(),
                    d.idxBztpLclsCd(), d.idxBztpMclsCd(), d.idxBztpSclsCd(),
                    d.idxBztpLclsNm(), d.idxBztpMclsNm(), d.idxBztpSclsNm(),
                    d.stdIdstClsfCd(), d.stdIdstClsfNm(),
                    d.frnrPsnlLmtRt(), d.trStopYn(), d.admnItemYn(),
                    d.lstgAbolDt(), d.sctsMketLstgDt());
            registerTag(seenTags, TagField.INDUSTRY_LCLS, d.idxBztpLclsNm());
            registerTag(seenTags, TagField.INDUSTRY_MCLS, d.idxBztpMclsNm());
            registerTag(seenTags, TagField.INDUSTRY_SCLS, d.idxBztpSclsNm());
            registerTag(seenTags, TagField.STD_INDUSTRY, d.stdIdstClsfNm());
        });
    }

    private void applyProductInfo(String ticker, Set<String> seenTags) {
        fetcher.fetchProductInfo(ticker).ifPresent(d -> {
            stockCommandService.applyProductInfo(ticker,
                    d.prdtName(), d.prdtAbrvName(), d.prdtEngName(),
                    d.shtnPdno(), d.prdtRiskGradCd(),
                    d.prdtClsfCd(), d.prdtClsfName());
            registerTag(seenTags, TagField.PRDT_CLSF, d.prdtClsfName());
        });
    }

    /**
     * 처음 본 (field, value) 조합만 마스터에 등록한다.
     */
    private void registerTag(Set<String> seen, TagField field, String value) {
        if (value == null || value.isBlank()) return;
        if (seen.add(field.name() + " " + value)) {
            tagValueService.registerIfAbsent(field.name(), value);
        }
    }
}
