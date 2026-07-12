package com.dove.custommetric.domain.spec;

/**
 * 커스텀 지표 계산식 트리의 노드.
 */
public sealed interface MetricNode
        permits AggNode, ConstNode, RefNode, RollMeanNode, EmaNode, CumProd1pNode, LagNode, BinaryNode {
}
