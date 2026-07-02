package com.dove.modelserving.domain.zone;

/**
 * 단일 진입존 조건 — 피처 하나를 임계값과 비교한다.
 *
 * @param feature 피처 이름(대문자, 컬럼명과 1:1)
 * @param prev    직전 거래일(SEQ-1) 값을 참조하면 true, 당일 값이면 false
 * @param op      비교 연산자
 * @param value   비교 임계값
 */
public record ZoneCondition(String feature, boolean prev, ZoneOperator op, double value) {

    /**
     * 당일 피처값({@code current})과 직전일 피처값({@code previous})으로 이 조건을 평가한다.
     * 참조하는 값이 없으면(미계산) false로 본다(fail-closed).
     */
    public boolean evaluate(java.util.Map<String, Double> current, java.util.Map<String, Double> previous) {
        Double actual = (prev ? previous : current).get(feature);
        if (actual == null) return false;
        return op.test(actual, value);
    }
}
