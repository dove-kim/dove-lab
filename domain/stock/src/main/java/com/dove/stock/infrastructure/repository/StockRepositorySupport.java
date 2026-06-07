package com.dove.stock.infrastructure.repository;

import com.dove.market.domain.enums.MarketType;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

import static com.dove.stock.domain.entity.QStock.stock;

/**
 * Stock QueryDSL 조회.
 */
@Repository
@RequiredArgsConstructor
public class StockRepositorySupport {

    private final JPAQueryFactory queryFactory;

    /**
     * 전체 종목 티커를 조회한다.
     */
    public List<String> findAllTickers() {
        return queryFactory.select(stock.ticker).from(stock).fetch();
    }

    /**
     * 시장 집합에 속하는 종목 티커를 조회한다.
     */
    public List<String> findTickersByMarkets(Collection<MarketType> markets) {
        return queryFactory.select(stock.ticker).from(stock)
                .where(stock.market.in(markets)).fetch();
    }
}
