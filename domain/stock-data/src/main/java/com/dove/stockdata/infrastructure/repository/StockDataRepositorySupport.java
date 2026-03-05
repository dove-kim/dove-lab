package com.dove.stockdata.infrastructure.repository;

import com.dove.stockdata.domain.entity.QStockData;
import com.dove.stockdata.domain.entity.StockData;
import com.dove.stockdata.domain.enums.MarketType;
import com.dove.stockdata.domain.repository.StockDataQueryRepository;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

/**
 * Querydsl 기반의 StockDataQueryRepository 구현체.
 */
@Repository
@RequiredArgsConstructor
public class StockDataRepositorySupport implements StockDataQueryRepository {

    private final JPAQueryFactory queryFactory;

    private static final QStockData stockData = QStockData.stockData;

    public List<StockData> findRecentStockData(MarketType marketType, String stockCode,
                                                LocalDate tradeDate, int limit) {
        return queryFactory
                .selectFrom(stockData)
                .where(
                        stockData.id.marketType.eq(marketType),
                        stockData.id.stockCode.eq(stockCode),
                        stockData.id.tradeDate.loe(tradeDate)
                )
                .orderBy(stockData.id.tradeDate.desc())
                .limit(limit)
                .fetch();
    }

    public List<String> findStockCodesByMarketTypeAndTradeDate(MarketType marketType, LocalDate tradeDate) {
        return queryFactory
                .selectDistinct(stockData.id.stockCode)
                .from(stockData)
                .where(
                        stockData.id.marketType.eq(marketType),
                        stockData.id.tradeDate.eq(tradeDate)
                )
                .fetch();
    }

    public List<LocalDate> findTradeDatesAfter(MarketType marketType, String stockCode, LocalDate tradeDate) {
        return queryFactory
                .selectDistinct(stockData.id.tradeDate)
                .from(stockData)
                .where(
                        stockData.id.marketType.eq(marketType),
                        stockData.id.stockCode.eq(stockCode),
                        stockData.id.tradeDate.gt(tradeDate)
                )
                .orderBy(stockData.id.tradeDate.asc())
                .fetch();
    }

    public List<LocalDate> findTradeDatesFrom(MarketType marketType, String stockCode, LocalDate fromDate) {
        return queryFactory
                .selectDistinct(stockData.id.tradeDate)
                .from(stockData)
                .where(
                        stockData.id.marketType.eq(marketType),
                        stockData.id.stockCode.eq(stockCode),
                        stockData.id.tradeDate.goe(fromDate)
                )
                .orderBy(stockData.id.tradeDate.asc())
                .fetch();
    }
}
