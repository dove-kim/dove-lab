package com.dove.stockcollection.application.port;

import java.time.LocalDate;

/**
 * KRX 상장 종목 정보.
 *
 * @param ticker          종목 코드
 * @param isin            ISIN 표준 코드
 * @param listingDate     상장일
 * @param secugrpNm       증권 그룹 구분명
 * @param kindStkCertTpNm 주식 종류 구분명
 */
public record StockListing(
        String ticker,
        String isin,
        LocalDate listingDate,
        String secugrpNm,
        String kindStkCertTpNm
) {}
