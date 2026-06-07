package com.dove.api.search.stock.dto;

import com.dove.stock.domain.entity.Stock;
import com.dove.stock.domain.entity.StockDetail;

import java.time.LocalDate;

/**
 * 종목 기본 정보 응답.
 *
 * @param ticker          종목 코드
 * @param name            종목명
 * @param isin            ISIN 코드
 * @param market          시장 구분
 * @param listingDate     상장일
 * @param secugrpNm       증권 그룹명
 * @param kindStkCertTpNm 주식 종류명
 * @param tradingHalt     거래정지 여부
 * @param adminItem       관리종목 여부
 */
public record StockResponse(
        String ticker,
        String name,
        String isin,
        String market,
        LocalDate listingDate,
        String secugrpNm,
        String kindStkCertTpNm,
        boolean tradingHalt,
        boolean adminItem
) {
    public static StockResponse from(Stock s, String name, StockDetail detail) {
        return new StockResponse(
                s.getTicker(),
                name,
                s.getIsin(),
                s.getMarket().name(),
                s.getListingDate(),
                s.getSecugrpNm(),
                s.getKindStkCertTpNm(),
                detail != null && "Y".equals(detail.getTrStopYn()),
                detail != null && "Y".equals(detail.getAdmnItemYn())
        );
    }
}
