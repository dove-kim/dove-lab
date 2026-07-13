package com.dove.modelserving.infrastructure.repository;

import com.dove.indicator.domain.entity.StockFeatureDaily;
import com.dove.indicator.domain.rank.entity.StockRankDaily;
import com.dove.stock.domain.enums.PriceType;
import com.dove.stock.domain.enums.StockExchange;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

import static com.dove.indicator.domain.entity.QStockFeatureDaily.stockFeatureDaily;
import static com.dove.indicator.domain.rank.entity.QStockRankDaily.stockRankDaily;

/**
 * 모델 채점이 참조하는 원천(피처 거래일·wide 피처·wide 순위) 조회 지원.
 */
@Repository
@RequiredArgsConstructor
public class ScoreSourceRepositorySupport {

    private final JPAQueryFactory queryFactory;

    /**
     * (거래소·가격유형)에서 afterExclusive 초과 ceilingInclusive 이하의 피처 거래일을
     * 오름차순 distinct로 반환한다(채점 대상 날짜).
     */
    public List<LocalDate> findScoreTradeDates(StockExchange exchange, PriceType priceType,
                                               LocalDate afterExclusive, LocalDate ceilingInclusive) {
        BooleanExpression where = stockFeatureDaily.id.exchange.eq(exchange)
                .and(stockFeatureDaily.id.priceType.eq(priceType))
                .and(stockFeatureDaily.id.tradeDate.loe(ceilingInclusive));
        if (afterExclusive != null) {
            where = where.and(stockFeatureDaily.id.tradeDate.gt(afterExclusive));
        }
        return queryFactory.select(stockFeatureDaily.id.tradeDate)
                .distinct()
                .from(stockFeatureDaily)
                .where(where)
                .orderBy(stockFeatureDaily.id.tradeDate.asc())
                .fetch();
    }

    /**
     * universe member 거래소들에서 afterExclusive 초과 ceilingInclusive 이하의 피처 거래일을
     * 오름차순 distinct로 반환한다(union 채점 대상 날짜).
     */
    public List<LocalDate> findScoreTradeDates(Collection<StockExchange> exchanges, PriceType priceType,
                                               LocalDate afterExclusive, LocalDate ceilingInclusive) {
        BooleanExpression where = stockFeatureDaily.id.exchange.in(exchanges)
                .and(stockFeatureDaily.id.priceType.eq(priceType))
                .and(stockFeatureDaily.id.tradeDate.loe(ceilingInclusive));
        if (afterExclusive != null) {
            where = where.and(stockFeatureDaily.id.tradeDate.gt(afterExclusive));
        }
        return queryFactory.select(stockFeatureDaily.id.tradeDate)
                .distinct()
                .from(stockFeatureDaily)
                .where(where)
                .orderBy(stockFeatureDaily.id.tradeDate.asc())
                .fetch();
    }

    /**
     * universe member 거래소들에서 최근 피처 거래일을 내림차순 distinct로 최대 limit개 반환한다(드라이런 표본 대상 날짜).
     */
    public List<LocalDate> findRecentTradeDates(Collection<StockExchange> exchanges, PriceType priceType, int limit) {
        return queryFactory.select(stockFeatureDaily.id.tradeDate)
                .distinct()
                .from(stockFeatureDaily)
                .where(stockFeatureDaily.id.exchange.in(exchanges)
                        .and(stockFeatureDaily.id.priceType.eq(priceType)))
                .orderBy(stockFeatureDaily.id.tradeDate.desc())
                .limit(limit)
                .fetch();
    }

    /**
     * (거래소·가격유형)에서 beforeExclusive 미만의 가장 가까운 피처 거래일을 반환한다(직전 거래일).
     * 없으면 null.
     */
    public LocalDate findPreviousTradeDate(StockExchange exchange, PriceType priceType, LocalDate beforeExclusive) {
        return queryFactory.select(stockFeatureDaily.id.tradeDate)
                .distinct()
                .from(stockFeatureDaily)
                .where(stockFeatureDaily.id.exchange.eq(exchange)
                        .and(stockFeatureDaily.id.priceType.eq(priceType))
                        .and(stockFeatureDaily.id.tradeDate.lt(beforeExclusive)))
                .orderBy(stockFeatureDaily.id.tradeDate.desc())
                .limit(1)
                .fetchFirst();
    }

    /**
     * universe member 거래소들을 통틀어 beforeExclusive 미만의 가장 가까운 피처 거래일을 반환한다(union 직전 거래일).
     * 없으면 null.
     */
    public LocalDate findPreviousTradeDate(Collection<StockExchange> exchanges, PriceType priceType,
                                           LocalDate beforeExclusive) {
        return queryFactory.select(stockFeatureDaily.id.tradeDate)
                .distinct()
                .from(stockFeatureDaily)
                .where(stockFeatureDaily.id.exchange.in(exchanges)
                        .and(stockFeatureDaily.id.priceType.eq(priceType))
                        .and(stockFeatureDaily.id.tradeDate.lt(beforeExclusive)))
                .orderBy(stockFeatureDaily.id.tradeDate.desc())
                .limit(1)
                .fetchFirst();
    }

    /**
     * 거래소·가격유형·날짜의 전 종목 wide 피처 행.
     */
    public List<StockFeatureDaily> findFeatures(StockExchange exchange, PriceType priceType, LocalDate date) {
        return queryFactory.selectFrom(stockFeatureDaily)
                .where(stockFeatureDaily.id.exchange.eq(exchange),
                        stockFeatureDaily.id.priceType.eq(priceType),
                        stockFeatureDaily.id.tradeDate.eq(date))
                .fetch();
    }

    /**
     * 거래소·가격유형·날짜의 전 종목 wide 순위 행.
     */
    public List<StockRankDaily> findRanks(StockExchange exchange, PriceType priceType, LocalDate date) {
        return queryFactory.selectFrom(stockRankDaily)
                .where(stockRankDaily.id.exchange.eq(exchange),
                        stockRankDaily.id.priceType.eq(priceType),
                        stockRankDaily.id.tradeDate.eq(date))
                .fetch();
    }
}
