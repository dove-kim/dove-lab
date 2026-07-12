package com.dove.api.search.stock.dto;

import com.dove.stock.domain.entity.Stock;
import com.dove.stock.domain.entity.StockDetail;
import com.dove.stock.domain.entity.StockPrice;

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
 * @param openPrice       최근 거래일 시가 (없으면 null)
 * @param highPrice       최근 거래일 고가 (없으면 null)
 * @param lowPrice        최근 거래일 저가 (없으면 null)
 * @param closePrice      최근 거래일 종가 (없으면 null)
 * @param volume          최근 거래일 거래량 (없으면 null)
 * @param prevClose       전일 종가 (등락률 계산용, 없으면 null)
 * @param marketCap       시가총액 (없으면 null)
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
        boolean adminItem,
        Long openPrice,
        Long highPrice,
        Long lowPrice,
        Long closePrice,
        Long volume,
        Long prevClose,
        Long marketCap
) {
    public static StockResponse from(Stock s, String name, StockDetail detail, StockPrice cur, StockPrice prev,
                                     Long marketCap) {
        return new StockResponse(
                s.getTicker(),
                name,
                s.getIsin(),
                s.getMarket().name(),
                s.getListingDate(),
                s.getSecugrpNm(),
                s.getKindStkCertTpNm(),
                detail != null && "Y".equals(detail.getTrStopYn()),
                detail != null && "Y".equals(detail.getAdmnItemYn()),
                cur != null ? cur.getOpenPrice() : null,
                cur != null ? cur.getHighPrice() : null,
                cur != null ? cur.getLowPrice() : null,
                cur != null ? cur.getClosePrice() : null,
                cur != null ? cur.getVolume() : null,
                prev != null ? prev.getClosePrice() : null,
                marketCap
        );
    }
}
