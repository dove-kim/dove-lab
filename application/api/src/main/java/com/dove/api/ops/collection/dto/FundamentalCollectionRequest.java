package com.dove.api.ops.collection.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

/**
 * 재무제표(DART) 재조회 요청 (연도 구간 — 일 단위는 무시, 연도만 사용).
 *
 * @param from 수집 시작(연도 기준)
 * @param to   수집 종료(연도 기준)
 */
public record FundamentalCollectionRequest(
        @NotNull LocalDate from,
        @NotNull LocalDate to
) {}
