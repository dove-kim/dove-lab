package com.dove.scheduler.dto;

import com.dove.market.domain.enums.MarketType;

import java.util.List;

/** 보정 스캔 한 번의 처리 결과. */
public record ScanResult(MarketType market, int total, int succeeded, List<String> failures) {

    public boolean hasFailures() {
        return !failures.isEmpty();
    }

    /** 처리 대상 없음(스킵) 결과 */
    public static ScanResult skipped(MarketType market) {
        return new ScanResult(market, 0, 0, List.of());
    }
}
