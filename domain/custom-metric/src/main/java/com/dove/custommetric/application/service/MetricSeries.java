package com.dove.custommetric.application.service;

import java.time.LocalDate;
import java.util.List;

/**
 * 지표 계산 결과 시계열 — 거래일 리스트와 그에 정렬된 값(미확정은 NaN).
 *
 * @param dates  거래일(오름차순)
 * @param values dates에 정렬된 계산값(NaN=미확정)
 */
public record MetricSeries(List<LocalDate> dates, double[] values) {
}
