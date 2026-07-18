package com.dove.portfolio.domain.enums;

/**
 * 포트폴리오 종목이 상장된 시장 — 현재가 조회 경로(국내 StockPrice vs 해외 수집)와 원통화를 결정한다.
 */
public enum PortfolioMarket {
    KOSPI("KRW", true),
    KOSDAQ("KRW", true),
    KONEX("KRW", true),
    NASDAQ("USD", false),
    NYSE("USD", false),
    AMEX("USD", false),
    HKEX("HKD", false),
    TSE("JPY", false),
    SSE("CNY", false),
    SZSE("CNY", false);

    private final String currency;
    private final boolean domestic;

    PortfolioMarket(String currency, boolean domestic) {
        this.currency = currency;
        this.domestic = domestic;
    }

    /**
     * 이 시장의 원통화 코드.
     */
    public String getCurrency() {
        return currency;
    }

    /**
     * KRX(국내) 시장 여부 — 기존 StockPrice 재사용 대상인지 판단한다.
     */
    public boolean isDomestic() {
        return domestic;
    }
}
