package com.dove.api.portfolio.dto;

import com.dove.portfolio.domain.enums.PortfolioMarket;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 종목 식별(시장·티커) 등록/갱신 요청 — (계좌, 종목) 기준 upsert.
 *
 * @param accountId 계좌 ID
 * @param symbol    종목명
 * @param market    상장 시장
 * @param ticker    시장 내 종목 코드
 */
public record AttachPortfolioHoldingRequest(
        @NotNull Long accountId,
        @NotBlank @Size(max = 100) String symbol,
        @NotNull PortfolioMarket market,
        @NotBlank @Size(max = 20) String ticker
) {}
