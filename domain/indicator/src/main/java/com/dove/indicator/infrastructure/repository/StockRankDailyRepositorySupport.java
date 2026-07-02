package com.dove.indicator.infrastructure.repository;

import com.dove.indicator.domain.rank.entity.StockRankDaily;
import com.dove.stock.domain.enums.PriceType;
import com.dove.stock.domain.enums.StockExchange;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

import static com.dove.indicator.domain.rank.entity.QStockRankDaily.stockRankDaily;

/**
 * wide 순위 행의 QueryDSL 기반 조회 지원.
 */
@Repository
@RequiredArgsConstructor
public class StockRankDailyRepositorySupport {

    private final JPAQueryFactory queryFactory;

    /**
     * 거래소·가격유형·날짜의 전 종목 wide 순위 행을 반환한다.
     */
    public List<StockRankDaily> findByExchangeAndPriceTypeAndDate(
            StockExchange exchange, PriceType priceType, LocalDate date) {
        return queryFactory.selectFrom(stockRankDaily)
                .where(stockRankDaily.id.exchange.eq(exchange),
                        stockRankDaily.id.priceType.eq(priceType),
                        stockRankDaily.id.tradeDate.eq(date))
                .fetch();
    }
}
