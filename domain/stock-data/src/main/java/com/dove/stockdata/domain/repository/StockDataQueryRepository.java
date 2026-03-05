package com.dove.stockdata.domain.repository;

import com.dove.stockdata.domain.entity.StockData;
import com.dove.stockdata.domain.enums.MarketType;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

/**
 * 주가 데이터 조회 전용 리포지토리 (포트).
 * Querydsl 기반의 동적 쿼리를 통해 주가 데이터를 조회한다.
 */
@Repository
public interface StockDataQueryRepository {
    /** 지정 날짜 이하의 최근 N건을 날짜 내림차순으로 조회한다. */
    List<StockData> findRecentStockData(MarketType marketType, String stockCode,
                                        LocalDate tradeDate, int limit);

    /** 특정 날짜에 데이터가 존재하는 종목 코드 목록을 조회한다. */
    List<String> findStockCodesByMarketTypeAndTradeDate(MarketType marketType, LocalDate tradeDate);

    /** 지정 날짜 초과(exclusive)의 거래일을 오름차순으로 조회한다. */
    List<LocalDate> findTradeDatesAfter(MarketType marketType, String stockCode, LocalDate tradeDate);

    /** 지정 날짜 이상(inclusive)의 거래일을 오름차순으로 조회한다. */
    List<LocalDate> findTradeDatesFrom(MarketType marketType, String stockCode, LocalDate fromDate);
}
