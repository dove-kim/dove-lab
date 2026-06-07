package com.dove.api.ops.collection.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

/**
 * 종목 재조회 요청 (기간).
 */
public record StockCollectionRequest(
        @NotNull LocalDate from,
        @NotNull LocalDate to
) {}
