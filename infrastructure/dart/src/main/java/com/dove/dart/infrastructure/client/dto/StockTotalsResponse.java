package com.dove.dart.infrastructure.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * DART 주식총수현황(stockTotqySttus) 응답.
 *
 * @param status  결과 코드
 * @param message 결과 메시지
 * @param list    주식총수 항목 목록
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record StockTotalsResponse(
        @JsonProperty("status") String status,
        @JsonProperty("message") String message,
        @JsonProperty("list") List<StockTotalsItem> list
) {
}
