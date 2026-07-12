package com.dove.custommetric.domain.spec;

/**
 * 횡단 집계 방식 — universe 종목들을 하루 스칼라로 접는 방법.
 */
public enum MetricAgg {

    /**
     * colA의 universe 평균.
     */
    MEAN,

    /**
     * universe 중 colA &gt; colB인 종목 비율(0~1).
     */
    RATIO_GT,

    /**
     * universe 중 colA &gt; 0인 종목 비율(0~1) — 상승비율(breadth) 등.
     */
    RATIO_POS
}
