package com.dove.api.search.searchfilter.dto;

/**
 * 검색 필터 매칭 종목 결과.
 *
 * @param code       종목 코드
 * @param name       종목명
 * @param marketType 시장 구분
 * @param openPrice  시가
 * @param highPrice  고가
 * @param lowPrice   저가
 * @param closePrice 종가
 * @param volume     거래량
 * @param prevClose  전일 종가 (등락률 계산용, 없으면 null)
 * @param marketCap  시가총액 (없으면 null)
 */
public record StockMatchResult(
        String code,
        String name,
        String marketType,
        Long openPrice,
        Long highPrice,
        Long lowPrice,
        Long closePrice,
        Long volume,
        Long prevClose,
        Long marketCap
) {}
