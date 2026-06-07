package com.dove.screening.infrastructure.repository;

import com.dove.indicator.domain.enums.IndicatorType;
import com.dove.screening.domain.value.ComparisonCondition;
import com.dove.screening.domain.value.FilterChildOp;
import com.dove.screening.domain.value.FilterGroup;
import com.dove.screening.domain.value.FilterNode;
import com.dove.screening.domain.value.FilterNot;
import com.dove.screening.domain.value.FilterOperand;
import com.dove.screening.domain.value.FilterOperator;
import com.dove.screening.domain.value.FilterPriceField;
import com.dove.screening.domain.value.FilterRange;
import com.dove.screening.domain.value.IndicatorOperand;
import com.dove.screening.domain.value.MarketFilterCondition;
import com.dove.screening.domain.value.PriceOperand;
import com.dove.screening.domain.value.RangeCondition;
import com.dove.screening.domain.value.ThresholdCondition;
import com.dove.screening.domain.value.UnknownCondition;
import com.dove.screening.domain.value.VolumeOperand;
import com.dove.stock.domain.enums.StockExchange;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.core.types.dsl.NumberPath;

import java.util.List;
import java.util.Optional;

import static com.dove.indicator.domain.entity.QStockFeatureDaily.stockFeatureDaily;

/**
 * 검색식 모델을 STOCK_FEATURE_DAILY 대상 QueryDSL 조건으로 변환한다.
 * 컬럼으로 표현 못 하는 조건이 하나라도 있으면 빈 값을 반환해 호출 측이 인메모리 평가로 폴백하게 한다.
 */
final class StockFeatureFilterTranslator {

    private StockFeatureFilterTranslator() {
    }

    /**
     * 모델을 QueryDSL 조건으로 변환한다. 전부 변환 가능하면 조건을, 하나라도 불가하면 빈 값을 반환한다.
     */
    static Optional<BooleanExpression> translate(FilterNode root) {
        return Optional.ofNullable(node(root));
    }

    private static BooleanExpression node(FilterNode n) {
        return switch (n) {
            case FilterGroup g -> group(g);
            case FilterNot not -> {
                BooleanExpression inner = node(not.inner());
                yield inner == null ? null : inner.not();
            }
            case ThresholdCondition t -> {
                NumberExpression<Double> col = operand(t.operand());
                yield col == null ? null : col.isNotNull().and(compare(col, t.operator(), t.value()));
            }
            case RangeCondition r -> {
                NumberExpression<Double> col = operand(r.operand());
                yield col == null ? null : col.isNotNull().and(range(col, r.range()));
            }
            case ComparisonCondition c -> {
                NumberExpression<Double> l = operand(c.left());
                NumberExpression<Double> r = operand(c.right());
                yield (l == null || r == null) ? null
                        : l.isNotNull().and(r.isNotNull()).and(compareExpr(l, c.operator(), r));
            }
            case MarketFilterCondition m -> market(m);
            case UnknownCondition u -> null;
        };
    }

    private static BooleanExpression group(FilterGroup g) {
        if (g.children().isEmpty()) return null; // 빈 그룹은 폴백
        BooleanExpression result = node(g.children().get(0));
        if (result == null) return null;
        for (int i = 1; i < g.children().size(); i++) {
            BooleanExpression child = node(g.children().get(i));
            if (child == null) return null;
            result = combine(result, g.ops().get(i - 1), child);
        }
        return result;
    }

    private static BooleanExpression combine(BooleanExpression acc, FilterChildOp op, BooleanExpression child) {
        return switch (op) {
            case OR -> acc.or(child);
            case AND_NOT -> acc.and(child.not());
            case OR_NOT -> acc.or(child.not());
            case AND -> acc.and(child);
        };
    }

    private static BooleanExpression market(MarketFilterCondition m) {
        if (m.markets().isEmpty()) return null;
        List<StockExchange> exchanges = m.markets().stream().map(StockExchange::fromMarket).toList();
        return stockFeatureDaily.id.exchange.in(exchanges);
    }

    private static NumberExpression<Double> operand(FilterOperand o) {
        return switch (o) {
            case IndicatorOperand i -> indicatorCol(i.type());
            case PriceOperand p -> priceCol(p.field());
            case VolumeOperand v -> stockFeatureDaily.volume.castToNum(Double.class);
        };
    }

    private static BooleanExpression compare(NumberExpression<Double> col, FilterOperator op, double v) {
        return switch (op) {
            case GT -> col.gt(v);
            case GTE -> col.goe(v);
            case LT -> col.lt(v);
            case LTE -> col.loe(v);
            case EQ -> col.eq(v);
            case NEQ -> col.ne(v);
        };
    }

    private static BooleanExpression compareExpr(NumberExpression<Double> l, FilterOperator op, NumberExpression<Double> r) {
        return switch (op) {
            case GT -> l.gt(r);
            case GTE -> l.goe(r);
            case LT -> l.lt(r);
            case LTE -> l.loe(r);
            case EQ -> l.eq(r);
            case NEQ -> l.ne(r);
        };
    }

    private static BooleanExpression range(NumberExpression<Double> col, FilterRange r) {
        BooleanExpression lo = r.minInclusive() ? col.goe(r.min()) : col.gt(r.min());
        BooleanExpression hi = r.maxInclusive() ? col.loe(r.max()) : col.lt(r.max());
        return lo.and(hi);
    }

    private static NumberExpression<Double> priceCol(FilterPriceField field) {
        NumberPath<Long> p = switch (field) {
            case OPEN -> stockFeatureDaily.openPrice;
            case HIGH -> stockFeatureDaily.highPrice;
            case LOW -> stockFeatureDaily.lowPrice;
            case CLOSE -> stockFeatureDaily.closePrice;
        };
        return p.castToNum(Double.class);
    }

    /** IndicatorType → wide 컬럼 (숫자 지표만; IS_* 불리언은 null → 폴백). */
    private static NumberExpression<Double> indicatorCol(IndicatorType t) {
        NumberPath<Float> p = switch (t) {
            case SMA_5 -> stockFeatureDaily.sma5;
            case SMA_10 -> stockFeatureDaily.sma10;
            case SMA_20 -> stockFeatureDaily.sma20;
            case SMA_50 -> stockFeatureDaily.sma50;
            case SMA_60 -> stockFeatureDaily.sma60;
            case SMA_120 -> stockFeatureDaily.sma120;
            case SMA_200 -> stockFeatureDaily.sma200;
            case EMA_5 -> stockFeatureDaily.ema5;
            case EMA_10 -> stockFeatureDaily.ema10;
            case EMA_20 -> stockFeatureDaily.ema20;
            case EMA_60 -> stockFeatureDaily.ema60;
            case EMA_120 -> stockFeatureDaily.ema120;
            case EMA_200 -> stockFeatureDaily.ema200;
            case RSI_9 -> stockFeatureDaily.rsi9;
            case RSI_14 -> stockFeatureDaily.rsi14;
            case RSI_21 -> stockFeatureDaily.rsi21;
            case MACD_LINE -> stockFeatureDaily.macdLine;
            case MACD_SIGNAL -> stockFeatureDaily.macdSignal;
            case MACD_HISTOGRAM -> stockFeatureDaily.macdHistogram;
            case STOCHASTIC_K_14_7 -> stockFeatureDaily.stochasticK147;
            case STOCHASTIC_D_14_7 -> stockFeatureDaily.stochasticD147;
            case ADX_14 -> stockFeatureDaily.adx14;
            case PLUS_DI_14 -> stockFeatureDaily.plusDi14;
            case MINUS_DI_14 -> stockFeatureDaily.minusDi14;
            case VOLUME_RATIO_20 -> stockFeatureDaily.volumeRatio20;
            case OBV -> stockFeatureDaily.obv;
            case BB_UPPER_20 -> stockFeatureDaily.bbUpper20;
            case BB_MIDDLE_20 -> stockFeatureDaily.bbMiddle20;
            case BB_LOWER_20 -> stockFeatureDaily.bbLower20;
            case BB_PERCENT_B_20 -> stockFeatureDaily.bbPercentB20;
            case BB_WIDTH_20 -> stockFeatureDaily.bbWidth20;
            case ATR -> stockFeatureDaily.atr;
            case MFI -> stockFeatureDaily.mfi;
            case CCI -> stockFeatureDaily.cci;
            case WILLIAMS_R -> stockFeatureDaily.williamsR;
            case VOLATILITY_5D -> stockFeatureDaily.volatility5d;
            case VOLATILITY_20D -> stockFeatureDaily.volatility20d;
            case HIGH_20D_RATIO -> stockFeatureDaily.high20dRatio;
            case HIGH_52W_RATIO -> stockFeatureDaily.high52wRatio;
            case LOW_20D_RATIO -> stockFeatureDaily.low20dRatio;
            case VOLUME_MA20_RATIO -> stockFeatureDaily.volumeMa20Ratio;
            case GAP_OPEN -> stockFeatureDaily.gapOpen;
            case RET_1D -> stockFeatureDaily.ret1d;
            case RET_5D -> stockFeatureDaily.ret5d;
            case RET_10D -> stockFeatureDaily.ret10d;
            case BODY_RATIO -> stockFeatureDaily.bodyRatio;
            case LOWER_WICK -> stockFeatureDaily.lowerWick;
            case IS_52W_HIGH, IS_52W_LOW, IS_20D_HIGH, IS_20D_LOW -> null; // 불리언 — SQL 푸시 미지원, 폴백
        };
        return p == null ? null : p.castToNum(Double.class);
    }
}
