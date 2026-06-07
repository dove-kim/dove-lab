package com.dove.scheduler.dto;

import com.dove.stock.domain.enums.PriceType;
import com.dove.stock.domain.enums.StockExchange;

/**
 * 기술적 지표 계산 단위 그룹 (종목 · 거래소 · 가격유형).
 */
public record IndicatorGroup(String ticker, StockExchange exchange, PriceType priceType) {
}
