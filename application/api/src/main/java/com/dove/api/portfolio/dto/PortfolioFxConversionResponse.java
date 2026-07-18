package com.dove.api.portfolio.dto;

import com.dove.portfolio.domain.entity.PortfolioFxConversion;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 환전 응답.
 *
 * @param id           환전 ID
 * @param accountId    계좌 ID
 * @param account      계좌명
 * @param convDate     환전 일자
 * @param fromCurrency 보낸 통화 코드
 * @param fromAmount   보낸 금액(보낸 통화)
 * @param toCurrency   받은 통화 코드
 * @param toAmount     받은 금액(받은 통화)
 * @param fee          수수료(보낸 통화)
 * @param memo         메모
 */
public record PortfolioFxConversionResponse(
        Long id,
        Long accountId,
        String account,
        LocalDate convDate,
        String fromCurrency,
        BigDecimal fromAmount,
        String toCurrency,
        BigDecimal toAmount,
        Long fee,
        String memo
) {
    public static PortfolioFxConversionResponse of(PortfolioFxConversion c, String accountName) {
        return new PortfolioFxConversionResponse(c.getId(), c.getAccountId(), accountName, c.getConvDate(),
                c.getFromCurrency(), c.getFromAmount(), c.getToCurrency(), c.getToAmount(), c.getFee(), c.getMemo());
    }
}
