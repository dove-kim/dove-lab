package com.dove.portfolio.domain.value;

/**
 * 리밸런싱 계획의 전략 현금 라인 — 계좌의 특정 통화 잔액이 전략 현금.
 *
 * @param account   계좌명
 * @param currency  원통화 코드
 * @param weightPct 리밸런싱 목표 비중(%) — 슬롯 계산에선 무시하고 전액 반영
 */
public record RebalancePlanCash(String account, String currency, double weightPct) {}
