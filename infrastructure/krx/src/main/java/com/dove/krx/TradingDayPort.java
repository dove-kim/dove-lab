package com.dove.krx;

import com.dove.market.domain.enums.MarketType;

import java.time.LocalDate;
import java.util.List;

/**
 * KRX 상장 종목 조회 포트.
 */
public interface TradingDayPort {
    /**
     * 해당 날짜의 KRX 상장 종목 목록을 조회한다.
     * 데이터가 없는 날짜(미래·API 미제공·오류)는 빈 리스트를 반환한다.
     */
    List<StockListing> fetchListings(MarketType market, LocalDate date);
}
