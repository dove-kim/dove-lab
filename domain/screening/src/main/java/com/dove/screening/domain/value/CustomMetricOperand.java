package com.dove.screening.domain.value;

/**
 * 커스텀 지표 피연산자 — 미리 계산·저장된 지표 값을 참조한다(거래일당 시장 단일 스칼라).
 *
 * @param metricId 커스텀 지표 정의 ID
 * @param offset   거래일 오프셋 (0=기준일, 양수=미래, 음수=과거)
 */
public record CustomMetricOperand(long metricId, int offset) implements FilterOperand {
}
