package com.dove.stockcollection.application.dto;

import com.dove.stock.domain.enums.PriceType;

import java.time.LocalDate;

/**
 * 주가 수집 작업 단위. 종목·가격유형·기간 윈도우 하나를 나타낸다.
 */
public record CollectionUnit(String ticker, PriceType priceType, LocalDate from, LocalDate to) {
}
