package com.dove.custommetric.application.service;

import java.util.Set;

/**
 * 종목필터 ID를 universe 티커 집합으로 해석하는 제공자 — 계산기가 screening에 직접 의존하지 않게 하는 경계.
 */
@FunctionalInterface
public interface UniverseResolver {

    /**
     * 종목필터 ID의 universe 티커 집합을 반환한다. 없으면 빈 집합.
     */
    Set<String> tickers(long stockFilterId);
}
