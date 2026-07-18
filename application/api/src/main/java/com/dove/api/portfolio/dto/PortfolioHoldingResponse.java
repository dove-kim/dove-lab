package com.dove.api.portfolio.dto;

import com.dove.portfolio.domain.entity.PortfolioHolding;
import com.dove.portfolio.domain.enums.PortfolioMarket;

/**
 * 포트폴리오 종목 식별 응답.
 *
 * @param id        식별 정보 ID
 * @param accountId 계좌 ID
 * @param account   계좌명
 * @param symbol    종목명
 * @param market    상장 시장
 * @param ticker    시장 내 종목 코드
 * @param currency  원통화 코드(시장에서 파생)
 */
public record PortfolioHoldingResponse(
        Long id,
        Long accountId,
        String account,
        String symbol,
        PortfolioMarket market,
        String ticker,
        String currency
) {
    public static PortfolioHoldingResponse of(PortfolioHolding h, String accountName) {
        return new PortfolioHoldingResponse(h.getId(), h.getAccountId(), accountName, h.getSymbol(),
                h.getMarket(), h.getTicker(), h.currency());
    }
}
