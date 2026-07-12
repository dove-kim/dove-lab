package com.dove.api.search.searchfilter.dto;

/**
 * 검색 필터를 통과한 종목 한 건의 읽기 모델.
 */
public record MatchedStock(String ticker, String name, String market, Long openPrice, Long highPrice,
                           Long lowPrice, Long closePrice, Long volume, Long prevClose, Long marketCap) {
}
