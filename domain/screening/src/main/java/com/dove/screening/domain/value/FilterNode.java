package com.dove.screening.domain.value;

/**
 * 검색 필터 트리의 노드.
 */
public sealed interface FilterNode
        permits FilterGroup, FilterNot, ThresholdCondition, RangeCondition,
        ComparisonCondition, MarketFilterCondition, StockStatusCondition, UnknownCondition {
}
