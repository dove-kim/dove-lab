package com.dove.frankfurter.infrastructure.client;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Frankfurter 구간 환율 응답.
 *
 * @param amount 기준 금액(보통 1)
 * @param base   기준 통화
 * @param rates  고시일(ISO) → 대상 통화별 환율
 */
public record FrankfurterSeriesResponse(BigDecimal amount, String base, Map<String, Map<String, BigDecimal>> rates) {}
