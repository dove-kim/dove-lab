package com.dove.custommetric.domain.spec;

/**
 * 계산식 트리에서 필요한 최대 lookback(거래일)을 구하는 유틸 — 증분 계산 시 되짚을 구간 산정용.
 */
public final class MetricSpecWindows {

    private MetricSpecWindows() {
    }

    /**
     * 스펙(let 포함)이 요구하는 최대 lookback 거래일 수. 롤링·EMA 창과 시차의 합성 깊이를 보수적으로 합산한다.
     */
    public static int maxLookback(MetricSpec spec) {
        int max = depth(spec.root(), spec);
        if (spec.lets() != null) {
            for (MetricNode n : spec.lets().values()) max = Math.max(max, depth(n, spec));
        }
        return max;
    }

    /** 노드가 요구하는 lookback — 시계열 연산 창들이 중첩되면 합산(보수적). */
    private static int depth(MetricNode node, MetricSpec spec) {
        return switch (node) {
            case RollMeanNode r -> r.window() + depth(r.input(), spec);
            case EmaNode e -> e.window() + depth(e.input(), spec);
            case LagNode l -> l.periods() + depth(l.input(), spec);
            case CumProd1pNode c -> depth(c.input(), spec);
            case BinaryNode b -> Math.max(depth(b.left(), spec), depth(b.right(), spec));
            case RefNode r -> spec.lets() != null && spec.lets().containsKey(r.name())
                    ? depth(spec.lets().get(r.name()), spec) : 0;
            case AggNode a -> 0;
            case ConstNode c -> 0;
        };
    }
}
