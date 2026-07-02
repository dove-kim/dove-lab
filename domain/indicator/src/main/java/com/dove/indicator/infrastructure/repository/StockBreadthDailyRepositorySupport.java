package com.dove.indicator.infrastructure.repository;

import com.dove.stock.domain.enums.PriceType;
import com.dove.stock.domain.enums.StockExchange;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;

import static com.dove.indicator.domain.breadth.entity.QStockBreadthDaily.stockBreadthDaily;

/**
 * 당일 상승비율 행의 QueryDSL 기반 조회 지원.
 */
@Repository
@RequiredArgsConstructor
public class StockBreadthDailyRepositorySupport {

    private final JPAQueryFactory queryFactory;

    /**
     * 거래소·가격유형·날짜의 당일 상승비율을 반환한다. 없으면 null.
     */
    public Double findAdvanceRatio(StockExchange exchange, PriceType priceType, LocalDate date) {
        return queryFactory.select(stockBreadthDaily.advanceRatio)
                .from(stockBreadthDaily)
                .where(stockBreadthDaily.id.exchange.eq(exchange),
                        stockBreadthDaily.id.priceType.eq(priceType),
                        stockBreadthDaily.id.tradeDate.eq(date))
                .fetchOne();
    }
}
