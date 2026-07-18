package com.dove.frankfurter.infrastructure.client;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

/**
 * Frankfurter 환율 응답.
 *
 * @param amount 기준 금액(보통 1)
 * @param base   기준 통화
 * @param date   환율 고시 일자
 * @param rates  대상 통화별 환율
 */
public record FrankfurterResponse(BigDecimal amount, String base, LocalDate date, Map<String, BigDecimal> rates) {}
