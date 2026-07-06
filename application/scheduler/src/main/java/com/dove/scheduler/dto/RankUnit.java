package com.dove.scheduler.dto;

import com.dove.stock.domain.enums.MarketUniverse;
import com.dove.stock.domain.enums.PriceType;

/**
 * 횡단면 순위 계산 단위 (universe · 가격유형).
 */
public record RankUnit(MarketUniverse universe, PriceType priceType) {
}
