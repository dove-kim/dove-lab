package com.dove.api.search.searchfilter.dto;

/**
 * 검색 필터를 통과한 종목 한 건의 읽기 모델.
 *
 * @param ticker     종목 코드
 * @param name       종목명
 * @param market     시장 구분
 * @param openPrice  시가
 * @param highPrice  고가
 * @param lowPrice   저가
 * @param closePrice 종가
 * @param volume     거래량
 * @param prevClose  전일 종가
 * @param marketCap  시가총액
 * @param modelScore 표시 대상 모델 점수(정렬/필터에 쓰인 모델, 없으면 null)
 */
public record MatchedStock(String ticker, String name, String market, Long openPrice, Long highPrice,
                           Long lowPrice, Long closePrice, Long volume, Long prevClose, Long marketCap,
                           Double modelScore) {
}
