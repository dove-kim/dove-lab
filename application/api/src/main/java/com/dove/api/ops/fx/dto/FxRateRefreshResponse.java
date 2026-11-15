package com.dove.api.ops.fx.dto;

/**
 * 환율 수동 채우기 결과 응답.
 *
 * @param saved 새로 저장된 행 수
 */
public record FxRateRefreshResponse(int saved) {}
