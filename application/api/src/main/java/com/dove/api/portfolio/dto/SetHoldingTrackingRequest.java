package com.dove.api.portfolio.dto;

import jakarta.validation.constraints.NotNull;

/**
 * 배당 추적 대상 설정 요청.
 *
 * @param tracked 배당 화면 표시 대상 여부
 */
public record SetHoldingTrackingRequest(@NotNull Boolean tracked) {}
