package com.dove.portfolio.application.service;

import com.dove.portfolio.domain.entity.PortfolioFxRate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

/**
 * 통화별 환율 이력을 담아 임의 일자의 환율을 찾아주는 순수 조회 테이블.
 */
public final class FxRateTable {

    private static final String KRW = "KRW";

    private final Map<String, NavigableMap<LocalDate, BigDecimal>> byCurrency;

    private FxRateTable(Map<String, NavigableMap<LocalDate, BigDecimal>> byCurrency) {
        this.byCurrency = byCurrency;
    }

    /**
     * 환율 이력으로 조회 테이블을 만든다.
     */
    public static FxRateTable of(Collection<PortfolioFxRate> rates) {
        Map<String, NavigableMap<LocalDate, BigDecimal>> byCurrency = new LinkedHashMap<>();
        for (PortfolioFxRate r : rates) {
            byCurrency.computeIfAbsent(r.getCurrency(), c -> new TreeMap<>()).put(r.getRateDate(), r.getRate());
        }
        return new FxRateTable(byCurrency);
    }

    /**
     * 지정일 기준 원통화 1단위당 원화를 반환한다. 고시가 없는 날은 그 이전 최신값, 이력 시작 이전은 가장 이른 값으로 근사한다.
     */
    public BigDecimal rateOn(String currency, LocalDate date) {
        if (KRW.equals(currency)) {
            return BigDecimal.ONE;
        }
        NavigableMap<LocalDate, BigDecimal> history = byCurrency.get(currency);
        if (history == null || history.isEmpty()) {
            return BigDecimal.ONE;
        }
        Map.Entry<LocalDate, BigDecimal> floor = history.floorEntry(date);
        return floor != null ? floor.getValue() : history.firstEntry().getValue();
    }

    /**
     * 해당 통화의 가장 최근 고시 환율을 반환한다.
     */
    public BigDecimal latest(String currency) {
        if (KRW.equals(currency)) {
            return BigDecimal.ONE;
        }
        NavigableMap<LocalDate, BigDecimal> history = byCurrency.get(currency);
        if (history == null || history.isEmpty()) {
            return BigDecimal.ONE;
        }
        return history.lastEntry().getValue();
    }
}
