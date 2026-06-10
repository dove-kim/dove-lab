package com.dove.api.ops.collection.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

/**
 * 주가 재조회 요청.
 *
 * @param exchange         KOSPI/KOSDAQ/KONEX/NXT/INTEGRATED
 * @param from             수집 시작일
 * @param to               수집 종료일
 * @param adjustedFromYear 수정주가 재조회 시작 연도 (null이면 수정주가 재조회 생략)
 */
public record PriceCollectionRequest(
        @NotNull String exchange,
        @NotNull LocalDate from,
        @NotNull LocalDate to,
        Integer adjustedFromYear
) {}
