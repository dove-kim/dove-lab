package com.dove.custommetric.domain;

import com.dove.custommetric.domain.spec.AggNode;

/**
 * 횡단 집계 leaf(AggNode)를 거래일 정렬 시계열로 해석하는 제공자 — 평가기를 순수하게 유지하는 경계.
 * 실제 구현은 STOCK_FEATURE_DAILY를 universe 기준으로 집계한다.
 */
@FunctionalInterface
public interface AggResolver {

    /**
     * AggNode를 평가 구간 거래일에 정렬된 시계열(길이 = 거래일 수)로 해석한다. 값 없는 거래일은 NaN.
     */
    double[] resolve(AggNode node);
}
