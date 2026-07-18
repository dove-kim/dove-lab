package com.dove.api.portfolio.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 환전 생성 요청.
 *
 * @param accountId    계좌 ID
 * @param convDate     환전 일자
 * @param fromCurrency 보낸 통화 코드
 * @param fromAmount   보낸 금액(보낸 통화)
 * @param toCurrency   받은 통화 코드
 * @param toAmount     받은 금액(받은 통화)
 * @param fee          수수료(보낸 통화)
 * @param memo         메모
 */
public record CreatePortfolioFxConversionRequest(
        @NotNull Long accountId,
        @NotNull LocalDate convDate,
        @NotBlank @Size(max = 10) String fromCurrency,
        @NotNull @Positive BigDecimal fromAmount,
        @NotBlank @Size(max = 10) String toCurrency,
        @NotNull @Positive BigDecimal toAmount,
        @PositiveOrZero Long fee,
        @Size(max = 500) String memo
) {}
