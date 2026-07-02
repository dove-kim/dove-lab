package com.dove.stock.domain.value;

/**
 * 종목의 거래정지·관리종목 상태 플래그.
 *
 * @param tradingHalted 거래정지 여부
 * @param adminItem     관리종목 여부
 */
public record StockStatusFlags(boolean tradingHalted, boolean adminItem) {
}
