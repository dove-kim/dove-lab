package com.dove.api.ops.collection.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

/**
 * 일별 밸류에이션 재계산 요청.
 *
 * @param from 재계산 시작일
 * @param to   재계산 종료일
 */
public record ValuationCollectionRequest(
        @NotNull LocalDate from,
        @NotNull LocalDate to
) {}
