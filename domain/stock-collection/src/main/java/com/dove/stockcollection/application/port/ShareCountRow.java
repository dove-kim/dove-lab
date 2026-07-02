package com.dove.stockcollection.application.port;

/**
 * 거래소가 준 종목별 상장주식수 한 건.
 *
 * @param ticker       종목코드
 * @param listedShares 상장주식수
 */
public record ShareCountRow(String ticker, long listedShares) {
}
