package com.dove.api.search.stockfilter.dto;

import jakarta.validation.constraints.NotNull;

/**
 * 종목 필터 활성화 여부 설정 요청.
 *
 * @param enabled 활성화 여부
 */
public record SetEnabledRequest(@NotNull Boolean enabled) {}
