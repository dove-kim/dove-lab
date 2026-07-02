package com.dove.stock.application.service;

import com.dove.market.domain.enums.MarketType;
import com.dove.stock.domain.entity.Stock;
import com.dove.stock.domain.entity.StockDetail;
import com.dove.stock.domain.repository.StockDetailRepository;
import com.dove.stock.domain.repository.StockRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 종목 마스터·종목 상세 저장.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StockCommandService {

    private final StockRepository stockRepository;
    private final StockDetailRepository stockDetailRepository;

    /**
     * KRX 수집 데이터로 종목을 upsert한다.
     */
    @Transactional
    public void upsertFromKrx(String ticker, String isin, MarketType market,
                               LocalDate listingDate, String secugrpNm, String kindStkCertTpNm) {
        stockRepository.findById(ticker).ifPresentOrElse(
                existing -> existing.updateFromKrx(isin, listingDate, secugrpNm, kindStkCertTpNm),
                () -> stockRepository.save(
                        new Stock(ticker, isin, market, listingDate, secugrpNm, kindStkCertTpNm))
        );
    }

    /**
     * DART 고유번호(corp_code)를 보유 종목에 매핑한다(ticker→corpCode). 매핑된 종목 수를 반환한다.
     */
    @Transactional
    public int assignCorpCodes(Map<String, String> tickerToCorpCode) {
        int matched = 0;
        for (Map.Entry<String, String> e : tickerToCorpCode.entrySet()) {
            Stock stock = stockRepository.findById(e.getKey()).orElse(null);
            if (stock != null) {
                stock.assignCorpCode(e.getValue());
                matched++;
            }
        }
        return matched;
    }

    /** DB에 없는 종목만 insert한다. 기존 종목은 보존. 백필 전용. */
    @Transactional
    public void insertIfAbsent(List<Stock> stocks) {
        Set<String> existingTickers = stockRepository.findAllById(
                stocks.stream().map(Stock::getTicker).collect(Collectors.toList())
        ).stream().map(Stock::getTicker).collect(Collectors.toSet());

        List<Stock> toInsert = stocks.stream()
                .filter(s -> !existingTickers.contains(s.getTicker()))
                .toList();

        if (!toInsert.isEmpty()) {
            stockRepository.saveAll(toInsert);
            log.info("{}개 종목 신규 추가", toInsert.size());
        }
    }

    /**
     * KIS 주식기본조회 데이터를 upsert한다.
     */
    @Transactional
    public void applyStockInfo(String ticker,
                               Long listedShares, Long capitalAmount, Long faceValue,
                               String stockKindCd, String etfDvsnCd, String reitsKindCd,
                               String kospi200ItemYn,
                               String idxBztpLclsCd, String idxBztpMclsCd, String idxBztpSclsCd,
                               String idxBztpLclsNm, String idxBztpMclsNm, String idxBztpSclsNm,
                               String stdIdstClsfCd, String stdIdstClsfNm,
                               String frnrPsnlLmtRt, String trStopYn, String admnItemYn,
                               String lstgAbolDt, String sctsMketLstgDt) {
        StockDetail detail = stockDetailRepository.findById(ticker)
                .orElseGet(() -> stockDetailRepository.save(new StockDetail(ticker)));
        detail.applyStockInfo(listedShares, capitalAmount, faceValue,
                stockKindCd, etfDvsnCd, reitsKindCd, kospi200ItemYn,
                idxBztpLclsCd, idxBztpMclsCd, idxBztpSclsCd,
                idxBztpLclsNm, idxBztpMclsNm, idxBztpSclsNm,
                stdIdstClsfCd, stdIdstClsfNm,
                frnrPsnlLmtRt, trStopYn, admnItemYn, lstgAbolDt, sctsMketLstgDt);
    }

    /**
     * KIS 상품기본조회 데이터를 upsert한다.
     */
    @Transactional
    public void applyProductInfo(String ticker,
                                 String prdtName, String prdtAbrvName, String prdtEngName,
                                 String shtnPdno, String prdtRiskGradCd,
                                 String prdtClsfCd, String prdtClsfNm) {
        StockDetail detail = stockDetailRepository.findById(ticker)
                .orElseGet(() -> stockDetailRepository.save(new StockDetail(ticker)));
        detail.applyProductInfo(prdtName, prdtAbrvName, prdtEngName,
                shtnPdno, prdtRiskGradCd, prdtClsfCd, prdtClsfNm);
    }
}
