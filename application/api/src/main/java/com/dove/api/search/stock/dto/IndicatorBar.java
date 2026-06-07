package com.dove.api.search.stock.dto;

import java.util.Map;

/**
 * 일자별 지표 값 묶음.
 *
 * @param date   거래일
 * @param values 지표 코드별 값
 */
public record IndicatorBar(String date, Map<String, Double> values) {}
