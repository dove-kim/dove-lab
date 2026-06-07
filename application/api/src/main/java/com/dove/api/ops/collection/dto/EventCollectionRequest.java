package com.dove.api.ops.collection.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

/**
 * 권리 이벤트(KSD) 재조회 요청.
 */
public record EventCollectionRequest(
        @NotNull LocalDate from,
        @NotNull LocalDate to
) {}
