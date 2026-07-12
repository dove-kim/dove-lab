package com.dove.custommetric.domain.entity;

/**
 * 커스텀 지표의 출력 모양 — 저장 단위를 결정한다.
 */
public enum MetricShape {

    /**
     * 시장 단일 스칼라(거래일당 1값) — 레짐·breadth 등.
     */
    SERIES,

    /**
     * 종목별 값(종목×거래일) — per-종목 지표. (P2)
     */
    PANEL
}
