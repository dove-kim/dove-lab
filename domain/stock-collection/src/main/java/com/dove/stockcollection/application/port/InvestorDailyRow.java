package com.dove.stockcollection.application.port;

import java.time.LocalDate;

/**
 * 투자자매매동향 한 거래일 데이터.
 *
 * @param tradeDate       거래일
 * @param individualBuy   개인 매수 수량
 * @param individualSell  개인 매도 수량
 * @param institutionBuy  기관 매수 수량
 * @param institutionSell 기관 매도 수량
 * @param foreignBuy      외국인 매수 수량
 * @param foreignSell     외국인 매도 수량
 */
public record InvestorDailyRow(
        LocalDate tradeDate,
        long individualBuy, long individualSell,
        long institutionBuy, long institutionSell,
        long foreignBuy, long foreignSell
) {}
