package com.dove.api.ops.collection.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

/**
 * 투자자매매동향 재조회 요청 (기간).
 *
 * @param from 수집 시작일
 * @param to   수집 종료일
 */
public record InvestorCollectionRequest(
        @NotNull LocalDate from,
        @NotNull LocalDate to
) {}
