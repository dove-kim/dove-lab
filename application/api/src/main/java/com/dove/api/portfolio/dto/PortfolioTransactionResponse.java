package com.dove.api.portfolio.dto;

import com.dove.portfolio.domain.entity.PortfolioTransaction;
import com.dove.portfolio.domain.enums.TxType;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 포트폴리오 거래 응답.
 *
 * @param id        거래 ID
 * @param accountId 계좌 ID
 * @param account   계좌명
 * @param type      거래 유형
 * @param tradedAt  체결/발생 일자
 * @param symbol    종목명(입출금이면 null)
 * @param currency  원통화 코드
 * @param quantity  수량(원통화 종목 단위)
 * @param price     단가(원통화)
 * @param amount    거래 금액(거래 통화 기준, 크기)
 * @param fee       수수료(거래 통화)
 * @param tag       자유 태그
 * @param memo      메모
 */
public record PortfolioTransactionResponse(
        Long id,
        Long accountId,
        String account,
        TxType type,
        LocalDate tradedAt,
        String symbol,
        String currency,
        BigDecimal quantity,
        BigDecimal price,
        BigDecimal amount,
        Long fee,
        String tag,
        String memo
) {
    public static PortfolioTransactionResponse of(PortfolioTransaction t, String accountName) {
        return new PortfolioTransactionResponse(t.getId(), t.getAccountId(), accountName, t.getType(), t.getTradeDate(),
                t.getSymbol(), t.getCurrency(), t.getQuantity(), t.getPrice(), t.getAmount(),
                t.getFee(), t.getTag(), t.getMemo());
    }
}
