package com.dove.indicator.infrastructure.repository;

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

/**
 * 순위 계산이 참조하는 원천(지표 커서 프런티어·피처 거래일) 조회 지원.
 */
@Repository
@RequiredArgsConstructor
public class RankSourceRepositorySupport {

    private final JPAQueryFactory queryFactory;

    /**
     * universe member 거래소들의 STOCK_FEATURE_DAILY 최신 거래일을 반환한다(피처 없으면 null).
     * 지표가 계산된 가장 최근 거래일이므로 rank/채점의 상한(프런티어)이 된다.
     * 상장폐지·거래정지 종목은 최근 거래일에 행이 없어 이 값을 끌어내리지 못한다(전 종목 커서 min 방식과 달리 영향 없음).
     * 파이프라인이 지표→rank 순서라 이 거래일의 횡단면은 완전하다(지표 완료 후 rank 실행).
     */
    public LocalDate findIndicatorFrontier(Collection<StockExchange> exchanges, PriceType priceType) {
        return queryFactory.select(stockFeatureDaily.id.tradeDate.max())
                .from(stockFeatureDaily)
                .where(stockFeatureDaily.id.exchange.in(exchanges)
                        .and(stockFeatureDaily.id.priceType.eq(priceType)))
                .fetchOne();
    }

    /**
     * (거래소·가격유형)에서 afterExclusive 초과 ceilingInclusive 이하의 피처 거래일을
     * 오름차순 distinct로 반환한다 (순위 계산 대상 날짜).
     */
    public List<LocalDate> findFeatureTradeDates(StockExchange exchange, PriceType priceType,
                                                 LocalDate afterExclusive, LocalDate ceilingInclusive) {
        return findFeatureTradeDates(List.of(exchange), priceType, afterExclusive, ceilingInclusive);
    }

    /**
     * universe member 거래소들에서 afterExclusive 초과 ceilingInclusive 이하의 피처 거래일을
     * 오름차순 distinct로 반환한다 (union 순위 계산 대상 날짜).
     */
    public List<LocalDate> findFeatureTradeDates(Collection<StockExchange> exchanges, PriceType priceType,
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
}
