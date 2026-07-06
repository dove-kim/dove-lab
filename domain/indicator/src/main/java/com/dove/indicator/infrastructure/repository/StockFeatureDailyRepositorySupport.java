package com.dove.indicator.infrastructure.repository;

import com.dove.indicator.domain.entity.StockFeatureDaily;
import com.dove.stock.domain.enums.PriceType;
import com.dove.stock.domain.enums.StockExchange;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

import static com.dove.indicator.domain.entity.QStockFeatureDaily.stockFeatureDaily;

/**
 * wide 피처 행의 QueryDSL 기반 조회 지원.
 */
@Repository
@RequiredArgsConstructor
public class StockFeatureDailyRepositorySupport {

    private final JPAQueryFactory queryFactory;

    /** 거래소·가격유형·날짜의 전 종목 wide 피처 행. (검색 필터·ML 평가용) */
    public List<StockFeatureDaily> findByExchangeAndPriceTypeAndDate(
            StockExchange exchange, PriceType priceType, LocalDate date) {
        return queryFactory.selectFrom(stockFeatureDaily)
                .where(stockFeatureDaily.id.exchange.eq(exchange),
                        stockFeatureDaily.id.priceType.eq(priceType),
                        stockFeatureDaily.id.tradeDate.eq(date))
                .fetch();
    }

    /** 거래소 집합(universe)·가격유형·날짜의 전 종목 wide 피처 행. (union 순위·상승비율 계산용) */
    public List<StockFeatureDaily> findByExchangesAndPriceTypeAndDate(
            Collection<StockExchange> exchanges, PriceType priceType, LocalDate date) {
        return queryFactory.selectFrom(stockFeatureDaily)
                .where(stockFeatureDaily.id.exchange.in(exchanges),
                        stockFeatureDaily.id.priceType.eq(priceType),
                        stockFeatureDaily.id.tradeDate.eq(date))
                .fetch();
    }

    /** 거래소 집합(universe)·가격유형의 [from, to] 거래일 구간 전 종목 wide 피처 행 (거래일 오름차순). 순위 청크 계산용. */
    public List<StockFeatureDaily> findByExchangesAndPriceTypeAndDateBetween(
            Collection<StockExchange> exchanges, PriceType priceType, LocalDate from, LocalDate to) {
        return queryFactory.selectFrom(stockFeatureDaily)
                .where(stockFeatureDaily.id.exchange.in(exchanges),
                        stockFeatureDaily.id.priceType.eq(priceType),
                        stockFeatureDaily.id.tradeDate.goe(from),
                        stockFeatureDaily.id.tradeDate.loe(to))
                .orderBy(stockFeatureDaily.id.tradeDate.asc())
                .fetch();
    }

    /** 종목·거래소·가격유형의 최근 거래일 N개 wide 피처 행 (거래일 내림차순). */
    public List<StockFeatureDaily> findRecentByTicker(
            String ticker, StockExchange exchange, PriceType priceType, int limit) {
        return queryFactory.selectFrom(stockFeatureDaily)
                .where(stockFeatureDaily.id.ticker.eq(ticker),
                        stockFeatureDaily.id.exchange.eq(exchange),
                        stockFeatureDaily.id.priceType.eq(priceType))
                .orderBy(stockFeatureDaily.id.tradeDate.desc())
                .limit(limit)
                .fetch();
    }

    /** 종목·거래소·가격유형의 beforeExclusive 직전 거래일 N개 wide 피처 행 (거래일 내림차순). 과거 페이지네이션용. */
    public List<StockFeatureDaily> findBeforeByTicker(
            String ticker, StockExchange exchange, PriceType priceType, LocalDate beforeExclusive, int limit) {
        return queryFactory.selectFrom(stockFeatureDaily)
                .where(stockFeatureDaily.id.ticker.eq(ticker),
                        stockFeatureDaily.id.exchange.eq(exchange),
                        stockFeatureDaily.id.priceType.eq(priceType),
                        stockFeatureDaily.id.tradeDate.lt(beforeExclusive))
                .orderBy(stockFeatureDaily.id.tradeDate.desc())
                .limit(limit)
                .fetch();
    }
}
