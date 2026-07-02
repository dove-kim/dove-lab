package com.dove.modelserving.infrastructure.scorer;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

/**
 * 채점기로 보내는 한 종목·거래일의 피처 입력.
 *
 * @param ticker    종목 코드
 * @param tradeDate 거래일(ISO-8601 문자열)
 * @param features  피처 이름(소문자)→값. 결측은 미포함 또는 null
 */
public record PredictRow(
        @JsonProperty("ticker") String ticker,
        @JsonProperty("trade_date") String tradeDate,
        @JsonProperty("features") Map<String, Double> features) {
}
