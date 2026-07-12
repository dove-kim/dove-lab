package com.dove.screening.infrastructure.repository;

import com.dove.custommetric.domain.entity.QCustomMetricDaily;

/**
 * 검색식 SQL 변환 시 생성한 CUSTOM_METRIC_DAILY join 별칭 — 어느 거래일 오프셋·지표에 붙는지 함께 담는다.
 *
 * @param offset   기준일로부터의 거래일 오프셋 (이 별칭이 붙는 피처 행)
 * @param metricId 커스텀 지표 ID
 * @param alias    CUSTOM_METRIC_DAILY join 별칭
 */
record CustomMetricJoinAlias(int offset, long metricId, QCustomMetricDaily alias) {
}
