package com.dove.custommetric.domain.spec;

/**
 * 횡단 집계 leaf — 종목 패널(STOCK_FEATURE_DAILY 컬럼)을 universe 기준으로 하루 스칼라 시계열로 접는다.
 * 실제 시계열은 데이터 제공자가 채운다(평가기는 순수).
 *
 * @param agg              집계 방식
 * @param colA             집계 대상 컬럼명 (예: RET_1D, CLOSE)
 * @param colB             비교 대상 컬럼명 (RATIO_GT일 때만, 아니면 null)
 * @param universeFilterId universe를 정의하는 종목필터 ID
 */
public record AggNode(MetricAgg agg, String colA, String colB, long universeFilterId) implements MetricNode {
}
