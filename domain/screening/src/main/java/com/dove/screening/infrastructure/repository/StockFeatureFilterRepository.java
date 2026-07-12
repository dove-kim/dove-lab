package com.dove.screening.infrastructure.repository;

import com.dove.indicator.domain.entity.QStockFeatureDaily;
import com.dove.indicator.domain.rank.entity.QStockRankDaily;
import com.dove.custommetric.domain.entity.QCustomMetricDaily;
import com.dove.modelserving.domain.entity.QStockModelScore;
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
                    // 전일 종가(등락률용)를 SEQ-1 행에서 이어붙이는 별칭 (거래일 무관, 조건 아닌 표시용).
                    QStockFeatureDaily prev = new QStockFeatureDaily("prevBar");
                    JPAQuery<FeatureMatch> query = queryFactory
                            .select(Projections.constructor(FeatureMatch.class,
                                    stockFeatureDaily.id.ticker,
                                    stockFeatureDaily.id.exchange,
                                    stockFeatureDaily.openPrice,
                                    stockFeatureDaily.highPrice,
                                    stockFeatureDaily.lowPrice,
                                    stockFeatureDaily.closePrice,
                                    stockFeatureDaily.volume,
                                    prev.closePrice))
                            .from(stockFeatureDaily)
                            .leftJoin(prev).on(
                                    prev.id.ticker.eq(stockFeatureDaily.id.ticker),
                                    prev.id.exchange.eq(stockFeatureDaily.id.exchange),
                                    prev.id.priceType.eq(stockFeatureDaily.id.priceType),
                                    prev.seq.eq(stockFeatureDaily.seq.subtract(1)));
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
                    // 순위 별칭을 오프셋 피처 행의 (ticker,exchange,price_type,trade_date)로 left join.
                    for (RankJoinAlias ra : tr.rankAliases()) {
                        QStockFeatureDaily f = tr.offsetAliases().get(ra.offset());
                        QStockRankDaily a = ra.alias();
                        query.leftJoin(a).on(
                                a.id.ticker.eq(f.id.ticker),
                                a.id.exchange.eq(f.id.exchange),
                                a.id.priceType.eq(f.id.priceType),
                                a.id.tradeDate.eq(f.id.tradeDate));
                    }
                    // 커스텀 지표 별칭을 오프셋 피처 행의 거래일 + metric_id로 left join (거래일당 시장 단일 스칼라 — ticker·exchange 없음).
                    for (CustomMetricJoinAlias ca : tr.customMetricAliases()) {
                        QStockFeatureDaily f = tr.offsetAliases().get(ca.offset());
                        QCustomMetricDaily a = ca.alias();
                        query.leftJoin(a).on(
                                a.id.metricId.eq(ca.metricId()),
                                a.id.tradeDate.eq(f.id.tradeDate));
                    }
                    // 모델점수 별칭을 오프셋 피처 행 + model_id로 left join.
                    for (ModelScoreJoinAlias ma : tr.modelScoreAliases()) {
                        QStockFeatureDaily f = tr.offsetAliases().get(ma.offset());
                        QStockModelScore a = ma.alias();
                        query.leftJoin(a).on(
                                a.id.ticker.eq(f.id.ticker),
                                a.id.exchange.eq(f.id.exchange),
                                a.id.priceType.eq(f.id.priceType),
                                a.id.tradeDate.eq(f.id.tradeDate),
                                a.id.modelId.eq(ma.modelId()));
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
