package com.dove.modelserving.infrastructure.scorer;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 채점기 stdout이 돌려주는 한 종목·거래일의 점수.
 *
 * @param ticker    종목 코드
 * @param tradeDate 거래일(ISO-8601 문자열)
 * @param score     점수(0~1 또는 연속값). 존 미충족·결측이면 null
 */
public record ScoredRow(
        @JsonProperty("ticker") String ticker,
        @JsonProperty("trade_date") String tradeDate,
        @JsonProperty("score") Double score) {
}
