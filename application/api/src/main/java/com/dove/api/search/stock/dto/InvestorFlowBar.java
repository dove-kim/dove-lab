package com.dove.api.search.stock.dto;

/**
 * 일자별 투자자별 순매수.
 *
 * @param date           거래일
 * @param individualNet  개인 순매수 수량
 * @param institutionNet 기관 순매수 수량
 * @param foreignNet     외국인 순매수 수량
 */
public record InvestorFlowBar(String date, long individualNet, long institutionNet, long foreignNet) {
}
