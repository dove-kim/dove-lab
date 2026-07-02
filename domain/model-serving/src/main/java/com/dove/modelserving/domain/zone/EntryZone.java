package com.dove.modelserving.domain.zone;

import java.util.List;
import java.util.Map;

/**
 * AND로 결합된 진입존 조건 묶음 — 모든 조건을 만족하는 행만 채점 대상이다.
 *
 * @param conditions 결합할 조건들(빈 목록이면 어떤 행도 통과시키지 않음)
 */
public record EntryZone(List<ZoneCondition> conditions) {

    /**
     * 당일·직전일 피처값으로 모든 조건을 AND 평가한다. 조건이 하나도 없으면 false(fail-closed).
     */
    public boolean matches(Map<String, Double> current, Map<String, Double> previous) {
        if (conditions.isEmpty()) return false;
        for (ZoneCondition condition : conditions) {
            if (!condition.evaluate(current, previous)) return false;
        }
        return true;
    }
}
