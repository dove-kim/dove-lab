package com.dove.dart.infrastructure.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * DART 주식총수현황(stockTotqySttus) 항목.
 *
 * @param se                구분(보통주/우선주/합계)
 * @param nowToIsuStockQty  현재까지 발행한 주식의 총수
 * @param nowToDcrsStockQty 현재까지 감소한 주식의 총수
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record StockTotalsItem(
        @JsonProperty("se") String se,
        @JsonProperty("now_to_isu_stock_totqy") String nowToIsuStockQty,
        @JsonProperty("now_to_dcrs_stock_totqy") String nowToDcrsStockQty
) {
}
