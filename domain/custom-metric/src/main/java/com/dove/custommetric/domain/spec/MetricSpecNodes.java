package com.dove.custommetric.domain.spec;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 계산식 트리 순회 유틸 — 데이터 사전 로드를 위해 AggNode(횡단 집계 leaf)를 수집한다.
 */
public final class MetricSpecNodes {

    private MetricSpecNodes() {
    }

    /**
     * 스펙(let 포함)이 참조하는 서로 다른 AggNode 목록을 반환한다.
     */
    public static List<AggNode> aggNodes(MetricSpec spec) {
        Set<AggNode> found = new LinkedHashSet<>();
        collect(spec.root(), spec, found);
        if (spec.lets() != null) {
            for (MetricNode n : spec.lets().values()) collect(n, spec, found);
        }
        return new ArrayList<>(found);
    }

    private static void collect(MetricNode node, MetricSpec spec, Set<AggNode> found) {
        switch (node) {
            case AggNode a -> found.add(a);
            case ConstNode c -> {
            }
            case RefNode r -> {
                if (spec.lets() != null && spec.lets().containsKey(r.name())) {
                    collect(spec.lets().get(r.name()), spec, found);
                }
            }
            case RollMeanNode rm -> collect(rm.input(), spec, found);
            case EmaNode e -> collect(e.input(), spec, found);
            case CumProd1pNode cp -> collect(cp.input(), spec, found);
            case LagNode l -> collect(l.input(), spec, found);
            case BinaryNode b -> {
                collect(b.left(), spec, found);
                collect(b.right(), spec, found);
            }
        }
    }
}
