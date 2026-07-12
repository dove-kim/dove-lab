package com.dove.screening.domain.value;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * 필터 트리에서 피연산자 종류를 수집하는 정적 유틸 — 인메모리 폴백 시 필요한 부가 데이터(순위·모델점수) 판단용.
 */
public final class FilterOperands {

    private FilterOperands() {
    }

    /**
     * 트리에 순위(RankOperand) 조건이 하나라도 있으면 true.
     */
    public static boolean usesRank(FilterNode node) {
        boolean[] found = {false};
        walk(node, o -> {
            if (o instanceof RankOperand) found[0] = true;
        });
        return found[0];
    }

    /**
     * 트리에 종목 상태(StockStatusCondition) 조건이 하나라도 있으면 true. 그룹·부정 내부도 재귀 탐색한다.
     */
    public static boolean usesStockStatus(FilterNode node) {
        return switch (node) {
            case FilterGroup g -> g.children().stream().anyMatch(FilterOperands::usesStockStatus);
            case FilterNot not -> usesStockStatus(not.inner());
            case StockStatusCondition s -> true;
            default -> false;
        };
    }

    /**
     * 트리가 참조하는 모델 식별자 집합을 반환한다.
     */
    public static Set<Long> referencedModelIds(FilterNode node) {
        Set<Long> ids = new LinkedHashSet<>();
        walk(node, o -> {
            if (o instanceof ModelScoreOperand m) ids.add(m.modelId());
        });
        return ids;
    }

    /**
     * 트리가 참조하는 커스텀 지표 식별자 집합을 반환한다.
     */
    public static Set<Long> referencedCustomMetricIds(FilterNode node) {
        Set<Long> ids = new LinkedHashSet<>();
        walk(node, o -> {
            if (o instanceof CustomMetricOperand c) ids.add(c.metricId());
        });
        return ids;
    }

    private static void walk(FilterNode node, java.util.function.Consumer<FilterOperand> sink) {
        switch (node) {
            case FilterGroup g -> g.children().forEach(c -> walk(c, sink));
            case FilterNot not -> walk(not.inner(), sink);
            case ThresholdCondition t -> sink.accept(t.operand());
            case RangeCondition r -> sink.accept(r.operand());
            case ComparisonCondition c -> {
                sink.accept(c.left());
                sink.accept(c.right());
            }
            case MarketFilterCondition m -> {
            }
            case StockStatusCondition s -> {
            }
            case UnknownCondition u -> {
            }
        }
    }
}
