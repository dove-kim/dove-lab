package com.dove.screening.domain.value;

/**
 * 검색 필터 모델을 종목 컨텍스트(시장·지표·주가)에 대해 평가한다. 순수 함수 — DB·스프링 의존 없음.
 */
public final class FilterEvaluator {

    private FilterEvaluator() {
    }

    /**
     * 모델 노드를 컨텍스트에 대해 평가해 통과 여부를 반환한다.
     */
    public static boolean evaluate(FilterNode node, EvalContext ctx) {
        return switch (node) {
            case FilterGroup g -> group(g, ctx);
            case FilterNot not -> !evaluate(not.inner(), ctx);
            case ThresholdCondition t -> {
                Double v = resolve(t.operand(), ctx);
                yield v != null && t.operator().compare(v, t.value());
            }
            case RangeCondition r -> {
                Double v = resolve(r.operand(), ctx);
                yield v != null && r.range().contains(v);
            }
            case ComparisonCondition c -> {
                Double l = resolve(c.left(), ctx);
                Double r = resolve(c.right(), ctx);
                yield l != null && r != null && c.operator().compare(l, r);
            }
            case MarketFilterCondition m -> m.markets().contains(ctx.market());
            case StockStatusCondition s -> s.passes(ctx.tradingHalted(), ctx.adminItem());
            case UnknownCondition u -> false;
        };
    }

    private static boolean group(FilterGroup g, EvalContext ctx) {
        if (g.children().isEmpty()) return true;
        boolean result = evaluate(g.children().get(0), ctx);
        for (int i = 1; i < g.children().size(); i++) {
            result = g.ops().get(i - 1).combine(result, evaluate(g.children().get(i), ctx));
        }
        return result;
    }

    private static Double resolve(FilterOperand operand, EvalContext ctx) {
        // 오프셋 비교는 SQL self-join(push-down) 전용 — 인메모리 폴백은 단일 행만 보므로 미지원.
        if (operand.offset() != 0) return null;
        return switch (operand) {
            case IndicatorOperand i -> ctx.indicators() == null ? null : ctx.indicators().get(i.type());
            case PriceOperand p -> price(p.field(), ctx);
            case VolumeOperand v -> ctx.price() == null ? null : ctx.price().getVolume().doubleValue();
            case TurnoverOperand t -> ctx.price() == null || ctx.price().getTurnover() == null
                    ? null : ctx.price().getTurnover().doubleValue();
            case ModelScoreOperand m -> ctx.modelScores() == null ? null : ctx.modelScores().get(m.modelId());
            case RankOperand r -> ctx.ranks() == null ? null : ctx.ranks().get(r.type());
            case CustomMetricOperand cm -> ctx.customMetrics() == null ? null : ctx.customMetrics().get(cm.metricId());
        };
    }

    private static Double price(FilterPriceField field, EvalContext ctx) {
        if (ctx.price() == null) return null;
        Long val = switch (field) {
            case OPEN -> ctx.price().getOpenPrice();
            case HIGH -> ctx.price().getHighPrice();
            case LOW -> ctx.price().getLowPrice();
            case CLOSE -> ctx.price().getClosePrice();
        };
        return val == null ? null : val.doubleValue();
    }
}
