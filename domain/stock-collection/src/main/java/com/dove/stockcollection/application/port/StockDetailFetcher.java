package com.dove.stockcollection.application.port;

import java.util.Optional;

/**
 * KIS 종목 상세 조회 포트. 주식기본·상품기본 정보를 가져온다.
 */
public interface StockDetailFetcher {

    /**
     * 주식기본조회 결과를 반환한다. 실패하거나 데이터 없으면 empty.
     */
    Optional<StockInfoData> fetchStockInfo(String ticker);

    /**
     * 상품기본조회 결과를 반환한다. 실패하거나 데이터 없으면 empty.
     */
    Optional<StockProductData> fetchProductInfo(String ticker);
}
