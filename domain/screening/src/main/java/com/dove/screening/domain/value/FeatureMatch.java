package com.dove.screening.domain.value;

import com.dove.stock.domain.enums.StockExchange;

/**
 * 검색식에 매칭된 종목의 식별·표시 정보.
 *
 * @param ticker     종목 코드
 * @param exchange   거래소
 * @param closePrice 종가
 * @param volume     거래량
 */
public record FeatureMatch(String ticker, StockExchange exchange, Long closePrice, Long volume) {
}
