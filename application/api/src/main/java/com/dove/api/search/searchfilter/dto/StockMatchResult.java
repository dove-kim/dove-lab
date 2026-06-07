package com.dove.api.search.searchfilter.dto;

/**
 * 검색 필터 매칭 종목 결과.
 *
 * @param code       종목 코드
 * @param name       종목명
 * @param marketType 시장 구분
 * @param closePrice 종가
 * @param volume     거래량
 */
public record StockMatchResult(
        String code,
        String name,
        String marketType,
        Long closePrice,
        Long volume
) {}
