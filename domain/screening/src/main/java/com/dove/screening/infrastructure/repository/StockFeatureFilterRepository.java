package com.dove.screening.infrastructure.repository;

import com.dove.indicator.domain.entity.QStockFeatureDaily;
import com.dove.screening.domain.value.FeatureMatch;
import com.dove.screening.domain.value.FilterNode;
import com.dove.stock.domain.enums.PriceType;
import com.dove.stock.domain.enums.StockExchange;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.dove.indicator.domain.entity.QStockFeatureDaily.stockFeatureDaily;

/**
 * 검색식을 STOCK_FEATURE_DAILY 컬럼 조건으로 변환해 매칭 종목을 조회한다.
 */
@Repository
@RequiredArgsConstructor
public class StockFeatureFilterRepository {

    private final JPAQueryFactory queryFactory;

    /**
     * 검색식을 컬럼 조건으로 변환해 만족하는 wide 피처 행을 조회한다. 식을 컬럼으로 표현할 수 없으면 빈 값을
     * 반환한다 (호출 측 인메모리 폴백 신호).
     */
    public Optional<List<FeatureMatch>> findMatchingByExpression(Collection<StockExchange> exchanges,
                                                                 PriceType priceType, LocalDate date,
                                                                 FilterNode expression) {
        return StockFeatureFilterTranslator.translate(expression, stockFeatureDaily)
                .map(tr -> {
                    JPAQuery<FeatureMatch> query = queryFactory
                            .select(Projections.constructor(FeatureMatch.class,
                                    stockFeatureDaily.id.ticker,
                                    stockFeatureDaily.id.exchange,
                                    stockFeatureDaily.closePrice,
                                    stockFeatureDaily.volume))
                            .from(stockFeatureDaily);
                    // 오프셋(N일 전/후) 별칭을 SEQ 기준으로 self-join (없으면 NULL → 조건 false).
                    for (Map.Entry<Integer, QStockFeatureDaily> e : tr.offsetAliases().entrySet()) {
                        if (e.getKey() == 0) continue;
                        QStockFeatureDaily a = e.getValue();
                        query.leftJoin(a).on(
                                a.id.ticker.eq(stockFeatureDaily.id.ticker),
                                a.id.exchange.eq(stockFeatureDaily.id.exchange),
                                a.id.priceType.eq(stockFeatureDaily.id.priceType),
                                a.seq.eq(stockFeatureDaily.seq.add(e.getKey())));
                    }
                    return query.where(
                            stockFeatureDaily.id.exchange.in(exchanges),
                            stockFeatureDaily.id.priceType.eq(priceType),
                            stockFeatureDaily.id.tradeDate.eq(date),
                            tr.predicate())
                            .fetch();
                });
    }

    /**
     * 거래소 집합·가격유형·날짜의 전 종목 수를 반환한다 (검색 대상 모수).
     */
    public long countByExchangesAndDate(Collection<StockExchange> exchanges, PriceType priceType, LocalDate date) {
        Long n = queryFactory.select(stockFeatureDaily.count())
                .from(stockFeatureDaily)
                .where(stockFeatureDaily.id.exchange.in(exchanges),
                        stockFeatureDaily.id.priceType.eq(priceType),
                        stockFeatureDaily.id.tradeDate.eq(date))
                .fetchOne();
        return n == null ? 0 : n;
    }
}
