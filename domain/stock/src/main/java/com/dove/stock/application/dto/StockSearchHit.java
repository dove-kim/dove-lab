package com.dove.stock.application.dto;

/**
 * 종목 자동완성 검색 결과 한 건.
 *
 * @param ticker 종목 코드
 * @param name   종목명(약명 우선)
 * @param market 시장(KOSPI/KOSDAQ/KONEX)
 */
public record StockSearchHit(String ticker, String name, String market) {}
