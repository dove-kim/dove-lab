package com.dove.custommetric.infrastructure.repository;

import com.dove.custommetric.domain.spec.MetricAgg;
import com.dove.indicator.domain.entity.QStockFeatureDaily;
import com.dove.stock.domain.enums.PriceType;
import com.dove.stock.domain.enums.StockExchange;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 커스텀 지표 계산의 원천 조회 — 시장 거래일 축과 universe 횡단 집계를 STOCK_FEATURE_DAILY에서 얻는다.
 */
@Repository
@RequiredArgsConstructor
public class CustomMetricSourceSupport {

    private static final List<StockExchange> KRX = List.of(StockExchange.KOSPI, StockExchange.KOSDAQ);

    private final JPAQueryFactory queryFactory;

    /**
     * priceType의 시장(코스피·코스닥) 거래일을 fromInclusive~toInclusive 오름차순 distinct로 반환한다(지표 날짜 축).
     * fromInclusive가 null이면 하한 없이 전 이력.
     */
    public List<LocalDate> marketTradeDates(PriceType priceType, LocalDate fromInclusive, LocalDate toInclusive) {
        QStockFeatureDaily f = QStockFeatureDaily.stockFeatureDaily;
        BooleanExpression where = f.id.exchange.in(KRX).and(f.id.priceType.eq(priceType))
                .and(f.id.tradeDate.loe(toInclusive));
        if (fromInclusive != null) where = where.and(f.id.tradeDate.goe(fromInclusive));
        return queryFactory.select(f.id.tradeDate).distinct()
                .from(f).where(where).orderBy(f.id.tradeDate.asc()).fetch();
    }

    /**
     * universe(tickers)에 대해 거래일별 횡단 집계값을 반환한다. MEAN=colA 평균, RATIO_GT=colA&gt;colB 비율.
     */
    public Map<LocalDate, Double> aggregate(MetricAgg agg, String colA, String colB,
                                            Collection<String> tickers, PriceType priceType,
                                            LocalDate fromInclusive, LocalDate toInclusive) {
        QStockFeatureDaily f = QStockFeatureDaily.stockFeatureDaily;
        NumberExpression<Double> a = col(f, colA);
        BooleanExpression where = f.id.ticker.in(tickers).and(f.id.priceType.eq(priceType))
                .and(f.id.tradeDate.loe(toInclusive)).and(a.isNotNull());
        if (fromInclusive != null) where = where.and(f.id.tradeDate.goe(fromInclusive));

        NumberExpression<Double> valueExpr;
        if (agg == MetricAgg.RATIO_GT) {
            NumberExpression<Double> b = col(f, colB);
            where = where.and(b.isNotNull());
            valueExpr = new CaseBuilder().when(a.gt(b)).then(1.0).otherwise(0.0).avg();
        } else if (agg == MetricAgg.RATIO_POS) {
            valueExpr = new CaseBuilder().when(a.gt(0.0)).then(1.0).otherwise(0.0).avg();
        } else {
            valueExpr = a.avg();
        }

        List<Tuple> rows = queryFactory.select(f.id.tradeDate, valueExpr)
                .from(f).where(where).groupBy(f.id.tradeDate).fetch();

        Map<LocalDate, Double> result = new LinkedHashMap<>(rows.size() * 2);
        for (Tuple t : rows) {
            result.put(t.get(f.id.tradeDate), t.get(valueExpr));
        }
        return result;
    }

    /** 컬럼명 → STOCK_FEATURE_DAILY 숫자 컬럼(Double). 지원 안 하는 이름은 예외. */
    private NumberExpression<Double> col(QStockFeatureDaily f, String name) {
        return switch (name) {
            case "RET_1D" -> f.ret1d.castToNum(Double.class);
            case "RET_5D" -> f.ret5d.castToNum(Double.class);
            case "RET_10D" -> f.ret10d.castToNum(Double.class);
            case "OPEN", "OPEN_PRICE" -> f.openPrice.castToNum(Double.class);
            case "HIGH", "HIGH_PRICE" -> f.highPrice.castToNum(Double.class);
            case "LOW", "LOW_PRICE" -> f.lowPrice.castToNum(Double.class);
            case "CLOSE", "CLOSE_PRICE" -> f.closePrice.castToNum(Double.class);
            case "VOLUME" -> f.volume.castToNum(Double.class);
            case "TURNOVER" -> f.turnover.castToNum(Double.class);
            case "SMA_5" -> f.sma5.castToNum(Double.class);
            case "SMA_10" -> f.sma10.castToNum(Double.class);
            case "SMA_20" -> f.sma20.castToNum(Double.class);
            case "SMA_50" -> f.sma50.castToNum(Double.class);
            case "SMA_60" -> f.sma60.castToNum(Double.class);
            case "SMA_120" -> f.sma120.castToNum(Double.class);
            case "SMA_200" -> f.sma200.castToNum(Double.class);
            case "EMA_20" -> f.ema20.castToNum(Double.class);
            case "EMA_60" -> f.ema60.castToNum(Double.class);
            case "EMA_120" -> f.ema120.castToNum(Double.class);
            case "EMA_200" -> f.ema200.castToNum(Double.class);
            case "RSI_14" -> f.rsi14.castToNum(Double.class);
            case "HIGH_52W_RATIO" -> f.high52wRatio.castToNum(Double.class);
            case "HIGH_20D_RATIO" -> f.high20dRatio.castToNum(Double.class);
            case "VOLATILITY_20D" -> f.volatility20d.castToNum(Double.class);
            case "VOLATILITY_5D" -> f.volatility5d.castToNum(Double.class);
            default -> throw new IllegalArgumentException("지원하지 않는 컬럼: " + name);
        };
    }
}
