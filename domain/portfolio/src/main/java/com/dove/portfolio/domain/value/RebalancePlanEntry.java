package com.dove.portfolio.domain.value;

/**
 * 리밸런싱 계획의 목표 배분 항목.
 *
 * @param symbol    종목명
 * @param account   계좌명(보유) 또는 "신규"(직접 추가) — 불러올 때 매칭 키
 * @param currency  원통화 코드
 * @param targetPct 목표 비중(%)
 */
public record RebalancePlanEntry(String symbol, String account, String currency, double targetPct) {}
