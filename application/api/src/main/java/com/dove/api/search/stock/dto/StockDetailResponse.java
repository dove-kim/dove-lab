package com.dove.api.search.stock.dto;

import com.dove.stock.domain.entity.Stock;
import com.dove.stock.domain.entity.StockDetail;

import java.time.LocalDate;

/**
 * 종목 상세 정보 (Stock 기본 + STOCK_DETAIL 전체).
 */
public record StockDetailResponse(
        String ticker,
        String name,
        String market,
        String isin,
        LocalDate listingDate,
        String secugrpNm,
        String kindStkCertTpNm,
        Long listedShares,
        Long capitalAmount,
        Long faceValue,
        String idxBztpLclsNm,
        String idxBztpMclsNm,
        String idxBztpSclsNm,
        String stdIdstClsfNm,
        String prdtClsfNm,
        String kospi200ItemYn,
        String trStopYn,
        String admnItemYn,
        String frnrPsnlLmtRt,
        String prdtRiskGradCd,
        String prdtName,
        String prdtEngName,
        String lstgAbolDt,
        String sctsMketLstgDt
) {
    /**
     * Stock 기본 정보와 StockDetail(없으면 null)을 합쳐 응답을 만든다.
     */
    public static StockDetailResponse from(Stock s, StockDetail d) {
        String name = resolveName(s, d);
        if (d == null) {
            return new StockDetailResponse(
                    s.getTicker(), name, s.getMarket().name(), s.getIsin(), s.getListingDate(),
                    s.getSecugrpNm(), s.getKindStkCertTpNm(),
                    null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null);
        }
        return new StockDetailResponse(
                s.getTicker(), name, s.getMarket().name(), s.getIsin(), s.getListingDate(),
                s.getSecugrpNm(), s.getKindStkCertTpNm(),
                d.getListedShares(), d.getCapitalAmount(), d.getFaceValue(),
                d.getIdxBztpLclsNm(), d.getIdxBztpMclsNm(), d.getIdxBztpSclsNm(),
                d.getStdIdstClsfNm(), d.getPrdtClsfNm(),
                d.getKospi200ItemYn(), d.getTrStopYn(), d.getAdmnItemYn(),
                d.getFrnrPsnlLmtRt(), d.getPrdtRiskGradCd(),
                d.getPrdtName(), d.getPrdtEngName(), d.getLstgAbolDt(), d.getSctsMketLstgDt());
    }

    private static String resolveName(Stock s, StockDetail d) {
        if (d != null) {
            if (d.getPrdtAbrvName() != null && !d.getPrdtAbrvName().isBlank()) return d.getPrdtAbrvName();
            if (d.getPrdtName() != null && !d.getPrdtName().isBlank()) return d.getPrdtName();
        }
        return s.getTicker();
    }
}
