package com.dove.stock.infrastructure.repository;

import com.dove.stock.domain.entity.StockPrice;
import com.dove.stock.domain.enums.PriceType;
import com.dove.stock.domain.enums.StockExchange;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

import static com.dove.stock.domain.entity.QStockPrice.stockPrice;

/**
 * StockPrice QueryDSL 조회.
 */
@Repository
@RequiredArgsConstructor
public class StockPriceRepositorySupport {

    private final JPAQueryFactory queryFactory;

    /** 종목·거래소·가격유형의 fromInclusive~toInclusive 구간을 거래일 오름차순으로 최대 limit개 조회한다 (페이징용). */
    public List<StockPrice> findChunk(String ticker, StockExchange exchange, PriceType priceType,
                                      LocalDate fromInclusive, LocalDate toInclusive, int limit) {
        return queryFactory.selectFrom(stockPrice)
                .where(stockPrice.id.ticker.eq(ticker),
                        stockPrice.id.exchange.eq(exchange),
                        stockPrice.id.priceType.eq(priceType),
                        stockPrice.id.tradeDate.goe(fromInclusive),
                        stockPrice.id.tradeDate.loe(toInclusive))
                .orderBy(stockPrice.id.tradeDate.asc())
                .limit(limit)
                .fetch();
    }

    /** 종목·거래소·가격유형의 beforeExclusive 직전 거래일 N개 주가 (내림차순). 지표 lookback용. */
    public List<StockPrice> findBefore(String ticker, StockExchange exchange, PriceType priceType,
                                       LocalDate beforeExclusive, int limit) {
        return queryFactory.selectFrom(stockPrice)
                .where(stockPrice.id.ticker.eq(ticker),
                        stockPrice.id.exchange.eq(exchange),
                        stockPrice.id.priceType.eq(priceType),
                        stockPrice.id.tradeDate.lt(beforeExclusive))
                .orderBy(stockPrice.id.tradeDate.desc())
                .limit(limit)
                .fetch();
    }

    /** 종목·거래소·가격유형에서 beforeExclusive 직전까지의 거래일 수 (SEQ 부여용). */
    public long countBefore(String ticker, StockExchange exchange, PriceType priceType,
                            LocalDate beforeExclusive) {
        Long n = queryFactory.select(stockPrice.count())
                .from(stockPrice)
                .where(stockPrice.id.ticker.eq(ticker),
                        stockPrice.id.exchange.eq(exchange),
                        stockPrice.id.priceType.eq(priceType),
                        stockPrice.id.tradeDate.lt(beforeExclusive))
                .fetchOne();
        return n == null ? 0 : n;
    }

    /** 종목·거래소·가격유형의 최근 거래일 N개 주가 (내림차순). */
    public List<StockPrice> findRecentByTicker(String ticker, StockExchange exchange,
                                               PriceType priceType, int limit) {
        return queryFactory.selectFrom(stockPrice)
                .where(stockPrice.id.ticker.eq(ticker),
                        stockPrice.id.exchange.eq(exchange),
                        stockPrice.id.priceType.eq(priceType))
                .orderBy(stockPrice.id.tradeDate.desc())
                .limit(limit)
                .fetch();
    }

    /** 거래소·가격유형·날짜에 해당하는 전 종목 주가. (검색 필터 평가용) */
    public List<StockPrice> findByExchangesAndDate(Collection<StockExchange> exchanges,
                                                   PriceType priceType, LocalDate date) {
        return queryFactory.selectFrom(stockPrice)
                .where(stockPrice.id.exchange.in(exchanges),
                        stockPrice.id.priceType.eq(priceType),
                        stockPrice.id.tradeDate.eq(date))
                .fetch();
    }

    /** 거래소 집합에서 from~to 구간의 거래일 (오름차순, distinct). */
    public List<LocalDate> findTradeDatesInRange(Collection<StockExchange> exchanges,
                                                 PriceType priceType, LocalDate from, LocalDate to) {
        return queryFactory.select(stockPrice.id.tradeDate).distinct()
                .from(stockPrice)
                .where(stockPrice.id.exchange.in(exchanges),
                        stockPrice.id.priceType.eq(priceType),
                        stockPrice.id.tradeDate.between(from, to))
                .orderBy(stockPrice.id.tradeDate.asc())
                .fetch();
    }

    /** onOrBefore 이하의 offset번째(0-based) 최근 거래일. */
    public LocalDate findNthRecentTradeDate(Collection<StockExchange> exchanges,
                                            PriceType priceType, LocalDate onOrBefore, int offset) {
        List<LocalDate> dates = queryFactory.select(stockPrice.id.tradeDate).distinct()
                .from(stockPrice)
                .where(stockPrice.id.exchange.in(exchanges),
                        stockPrice.id.priceType.eq(priceType),
                        stockPrice.id.tradeDate.loe(onOrBefore))
                .orderBy(stockPrice.id.tradeDate.desc())
                .offset(offset)
                .limit(1)
                .fetch();
        return dates.isEmpty() ? null : dates.get(0);
    }
}
