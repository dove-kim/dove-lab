package com.dove.stockprice.infrastructure.repository;

import com.dove.market.domain.enums.MarketType;
import com.dove.stockprice.domain.entity.DailyStockPrice;
import com.querydsl.jpa.impl.JPAQueryFactory;

import static com.dove.stockprice.domain.entity.QDailyStockPrice.dailyStockPrice;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

/** Querydsl 기반의 주가 데이터 조회 Repository. */
@Repository
@RequiredArgsConstructor
public class DailyStockPriceRepositorySupport {

    private final JPAQueryFactory queryFactory;

    public List<DailyStockPrice> findRecentDailyStockPrice(MarketType marketType, String stockCode,
                                                           LocalDate tradeDate, int limit) {
        return queryFactory
                .selectFrom(dailyStockPrice)
                .where(
                        dailyStockPrice.id.marketType.eq(marketType),
                        dailyStockPrice.id.stockCode.eq(stockCode),
                        dailyStockPrice.id.tradeDate.loe(tradeDate)
                )
                .orderBy(dailyStockPrice.id.tradeDate.desc())
                .limit(limit)
                .fetch();
    }

    public List<String> findStockCodesByMarketTypeAndTradeDate(MarketType marketType, LocalDate tradeDate) {
        return queryFactory
                .selectDistinct(dailyStockPrice.id.stockCode)
                .from(dailyStockPrice)
                .where(
                        dailyStockPrice.id.marketType.eq(marketType),
                        dailyStockPrice.id.tradeDate.eq(tradeDate)
                )
                .fetch();
    }

    public List<LocalDate> findDistinctTradeDatesInRange(MarketType marketType, LocalDate from, LocalDate to) {
        return queryFactory
                .selectDistinct(dailyStockPrice.id.tradeDate)
                .from(dailyStockPrice)
                .where(
                        dailyStockPrice.id.marketType.eq(marketType),
                        dailyStockPrice.id.tradeDate.between(from, to)
                )
                .fetch();
    }

}
