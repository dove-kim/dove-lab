package com.dove.api.ops.collection.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

/**
 * 주가 재조회 요청.
 */
public record PriceCollectionRequest(
        @NotNull String exchange,   // KOSPI/KOSDAQ/KONEX/NXT/INTEGRATED
        @NotNull LocalDate from,
        @NotNull LocalDate to,
        Integer adjustedFromYear    // 수정주가 재조회 시작 연도. null이면 재조회 안 함.
) {
    /**
     * 수정주가 재조회 하한 날짜. null이면 재조회 생략.
     */
    public LocalDate adjustedFrom() {
        return adjustedFromYear == null ? null : LocalDate.of(adjustedFromYear, 1, 1);
    }
}
