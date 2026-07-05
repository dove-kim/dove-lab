package com.dove.screening.domain.value;

import com.dove.stock.domain.enums.StockExchange;

/**
 * 검색식에 매칭된 종목의 식별·표시 정보.
 *
 * @param ticker     종목 코드
 * @param exchange   거래소
 * @param openPrice  시가
 * @param highPrice  고가
 * @param lowPrice   저가
 * @param closePrice 종가
 * @param volume     거래량
 * @param prevClose  전일 종가 (등락률 계산용, 없으면 null)
 */
public record FeatureMatch(String ticker, StockExchange exchange, Long openPrice, Long highPrice,
                           Long lowPrice, Long closePrice, Long volume, Long prevClose) {
}
