package com.dove.api.ops.fx.dto;

/**
 * 환율·종가 수동 채우기 결과 응답.
 *
 * @param savedRates  새로 저장된 환율 행 수
 * @param savedQuotes 새로 저장된 해외 종가 행 수
 */
public record FxRateRefreshResponse(int savedRates, int savedQuotes) {}
