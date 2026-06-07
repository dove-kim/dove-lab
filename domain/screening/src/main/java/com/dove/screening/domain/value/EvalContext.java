package com.dove.screening.domain.value;

import com.dove.indicator.domain.enums.IndicatorType;
import com.dove.market.domain.enums.MarketType;
import com.dove.stock.domain.entity.StockPrice;

import java.util.Map;

/**
 * 필터 식 평가 컨텍스트 — 한 종목의 시장·지표·당일 주가.
 *
 * @param market     시장
 * @param indicators 지표값 맵
 * @param price      당일 주가
 */
public record EvalContext(MarketType market, Map<IndicatorType, Double> indicators, StockPrice price) {
}
