package com.dove.api.portfolio.dto;

import com.dove.portfolio.domain.enums.TxType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 거래 생성 요청.
 *
 * @param accountId 계좌 ID
 * @param type      거래 유형
 * @param tradedAt  체결/발생 일자
 * @param symbol    종목명(입출금이면 null)
 * @param currency  원통화 코드(KRW/USD 등)
 * @param quantity  수량(원통화 종목 단위)
 * @param price     단가(원통화)
 * @param amount    거래 금액(거래 통화 기준, 크기)
 * @param fee       수수료(거래 통화)
 * @param tag       자유 태그
 * @param memo      메모
 */
public record CreatePortfolioTransactionRequest(
        @NotNull Long accountId,
        @NotNull TxType type,
        @NotNull LocalDate tradedAt,
        @Size(max = 100) String symbol,
        @NotBlank @Size(max = 10) String currency,
        @PositiveOrZero BigDecimal quantity,
        @PositiveOrZero BigDecimal price,
        @NotNull @PositiveOrZero BigDecimal amount,
        @PositiveOrZero Long fee,
        @Size(max = 50) String tag,
        @Size(max = 500) String memo
) {}
