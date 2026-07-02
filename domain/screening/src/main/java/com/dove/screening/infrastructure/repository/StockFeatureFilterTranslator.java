package com.dove.screening.infrastructure.repository;

import com.dove.indicator.domain.breadth.entity.QStockBreadthDaily;
import com.dove.indicator.domain.enums.IndicatorType;
import com.dove.indicator.domain.entity.QStockFeatureDaily;
import com.dove.indicator.domain.rank.entity.QStockRankDaily;
import com.dove.indicator.domain.rank.enums.RankType;
import com.dove.modelserving.domain.entity.QStockModelScore;
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
import com.dove.screening.domain.value.ModelScoreOperand;
import com.dove.screening.domain.value.PriceOperand;
import com.dove.screening.domain.value.BreadthOperand;
import com.dove.screening.domain.value.RangeCondition;
import com.dove.screening.domain.value.RankOperand;
import com.dove.screening.domain.value.StockStatusCondition;
import com.dove.screening.domain.value.ThresholdCondition;
import com.dove.screening.domain.value.UnknownCondition;
import com.dove.screening.domain.value.VolumeOperand;
import com.dove.stock.domain.enums.StockExchange;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.core.types.dsl.NumberPath;

import java.util.List;
import java.util.Optional;

/**
 * 검색식 모델을 STOCK_FEATURE_DAILY 대상 QueryDSL 조건으로 변환한다.
 * 컬럼으로 표현 못 하는 조건이 하나라도 있으면 빈 값을 반환해 호출 측이 인메모리 평가로 폴백하게 한다.
 * 오프셋(N일 전/후) 피연산자는 SEQ 기준 self-join 별칭으로, 순위·상승비율·모델점수는 별도 테이블 left join 별칭으로 변환한다.
 */
final class StockFeatureFilterTranslator {

    private StockFeatureFilterTranslator() {
    }

    /**
     * 모델을 QueryDSL 조건으로 변환한다. 전부 변환 가능하면 술어+join 별칭을, 하나라도 불가하면 빈 값을 반환한다.
     */
    static Optional<TranslatedFilter> translate(FilterNode root, QStockFeatureDaily base) {
        TranslationAliases aliases = new TranslationAliases(base);
        BooleanExpression predicate = node(root, base, aliases);
        return predicate == null ? Optional.empty()
                : Optional.of(new TranslatedFilter(predicate, aliases.offsetAliases(),
                        aliases.rankAliases(), aliases.breadthAliases(), aliases.modelScoreAliases()));
    }

    private static BooleanExpression node(FilterNode n, QStockFeatureDaily base, TranslationAliases aliases) {
        return switch (n) {
            case FilterGroup g -> group(g, base, aliases);
            case FilterNot not -> {
                BooleanExpression inner = node(not.inner(), base, aliases);
                yield inner == null ? null : inner.not();
            }
            case ThresholdCondition t -> {
                NumberExpression<Double> col = operand(t.operand(), base, aliases);
                yield col == null ? null : col.isNotNull().and(compare(col, t.operator(), t.value()));
            }
            case RangeCondition r -> {
                NumberExpression<Double> col = operand(r.operand(), base, aliases);
                yield col == null ? null : col.isNotNull().and(range(col, r.range()));
            }
            case ComparisonCondition c -> {
                NumberExpression<Double> l = operand(c.left(), base, aliases);
                NumberExpression<Double> r = operand(c.right(), base, aliases);
                yield (l == null || r == null) ? null
                        : l.isNotNull().and(r.isNotNull()).and(compareExpr(l, c.operator(), r));
            }
            case MarketFilterCondition m -> market(m, base);
            case StockStatusCondition s -> null; // DB 푸시다운 불가 → 폴백
            case UnknownCondition u -> null;
        };
    }

    private static BooleanExpression group(FilterGroup g, QStockFeatureDaily base, TranslationAliases aliases) {
        if (g.children().isEmpty()) return null; // 빈 그룹은 폴백
        BooleanExpression result = node(g.children().get(0), base, aliases);
        if (result == null) return null;
        for (int i = 1; i < g.children().size(); i++) {
            BooleanExpression child = node(g.children().get(i), base, aliases);
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

    private static BooleanExpression market(MarketFilterCondition m, QStockFeatureDaily base) {
        if (m.markets().isEmpty()) return null;
        List<StockExchange> exchanges = m.markets().stream().map(StockExchange::fromMarket).toList();
        return base.id.exchange.in(exchanges);
    }

    private static NumberExpression<Double> operand(FilterOperand o, QStockFeatureDaily base, TranslationAliases aliases) {
        return switch (o) {
            case IndicatorOperand i -> indicatorCol(aliases.featureAlias(i.offset()), i.type());
            case PriceOperand p -> priceCol(aliases.featureAlias(p.offset()), p.field());
            case VolumeOperand v -> aliases.featureAlias(v.offset()).volume.castToNum(Double.class);
            case ModelScoreOperand m -> aliases.modelScoreAlias(m.offset(), m.modelId()).score.castToNum(Double.class);
            case RankOperand r -> rankCol(aliases.rankAlias(r.offset()), r.type());
            case BreadthOperand b -> aliases.breadthAlias(b.offset()).advanceRatio;
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

    private static NumberExpression<Double> priceCol(QStockFeatureDaily q, FilterPriceField field) {
        NumberPath<Long> p = switch (field) {
            case OPEN -> q.openPrice;
            case HIGH -> q.highPrice;
            case LOW -> q.lowPrice;
            case CLOSE -> q.closePrice;
        };
        return p.castToNum(Double.class);
    }

    /** RankType → STOCK_RANK_DAILY 컬럼. */
    private static NumberExpression<Double> rankCol(QStockRankDaily q, RankType t) {
        NumberPath<Float> p = switch (t) {
            case RANK_RET_1D -> q.rankRet1d;
            case RANK_RET_5D -> q.rankRet5d;
            case RANK_RET_10D -> q.rankRet10d;
            case RANK_VOLUME_RATIO_20 -> q.rankVolumeRatio20;
            case RANK_RSI_14 -> q.rankRsi14;
            case RANK_MACD_HISTOGRAM -> q.rankMacdHistogram;
            case RANK_HIGH_52W_RATIO -> q.rankHigh52wRatio;
            case RANK_VOLATILITY_20D -> q.rankVolatility20d;
            case RANK_TURNOVER -> q.rankTurnover;
        };
        return p.castToNum(Double.class);
    }

    /** IndicatorType → wide 컬럼 (숫자 지표만; IS_* 불리언은 null → 폴백). */
    private static NumberExpression<Double> indicatorCol(QStockFeatureDaily q, IndicatorType t) {
        NumberPath<Float> p = switch (t) {
            case SMA_5 -> q.sma5;
            case SMA_10 -> q.sma10;
            case SMA_20 -> q.sma20;
            case SMA_50 -> q.sma50;
            case SMA_60 -> q.sma60;
            case SMA_120 -> q.sma120;
            case SMA_200 -> q.sma200;
            case EMA_5 -> q.ema5;
            case EMA_10 -> q.ema10;
            case EMA_20 -> q.ema20;
            case EMA_60 -> q.ema60;
            case EMA_120 -> q.ema120;
            case EMA_200 -> q.ema200;
            case RSI_9 -> q.rsi9;
            case RSI_14 -> q.rsi14;
            case RSI_21 -> q.rsi21;
            case MACD_LINE -> q.macdLine;
            case MACD_SIGNAL -> q.macdSignal;
            case MACD_HISTOGRAM -> q.macdHistogram;
            case STOCHASTIC_K_14_7 -> q.stochasticK147;
            case STOCHASTIC_D_14_7 -> q.stochasticD147;
            case ADX_14 -> q.adx14;
            case PLUS_DI_14 -> q.plusDi14;
            case MINUS_DI_14 -> q.minusDi14;
            case VOLUME_RATIO_20 -> q.volumeRatio20;
            case OBV -> q.obv;
            case BB_UPPER_20 -> q.bbUpper20;
            case BB_MIDDLE_20 -> q.bbMiddle20;
            case BB_LOWER_20 -> q.bbLower20;
            case BB_PERCENT_B_20 -> q.bbPercentB20;
            case BB_WIDTH_20 -> q.bbWidth20;
            case ATR -> q.atr;
            case MFI -> q.mfi;
            case CCI -> q.cci;
            case WILLIAMS_R -> q.williamsR;
            case VOLATILITY_5D -> q.volatility5d;
            case VOLATILITY_20D -> q.volatility20d;
            case HIGH_20D_RATIO -> q.high20dRatio;
            case HIGH_52W_RATIO -> q.high52wRatio;
            case LOW_20D_RATIO -> q.low20dRatio;
            case VOLUME_MA20_RATIO -> q.volumeMa20Ratio;
            case GAP_OPEN -> q.gapOpen;
            case RET_1D -> q.ret1d;
            case RET_5D -> q.ret5d;
            case RET_10D -> q.ret10d;
            case BODY_RATIO -> q.bodyRatio;
            case LOWER_WICK -> q.lowerWick;
            case UPPER_WICK_RATIO -> q.upperWickRatio;
            case CLOSE_POS -> q.closePos;
            case BULLISH_ENGULFING -> q.bullishEngulfing;
            case BEARISH_ENGULFING -> q.bearishEngulfing;
            case BREAKOUT_20D -> q.breakout20d;
            case IS_52W_HIGH, IS_52W_LOW, IS_20D_HIGH, IS_20D_LOW -> null; // 불리언 — SQL 푸시 미지원, 폴백
        };
        return p == null ? null : p.castToNum(Double.class);
    }
}
