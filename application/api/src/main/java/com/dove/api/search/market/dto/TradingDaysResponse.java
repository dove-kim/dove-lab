package com.dove.api.search.market.dto;

import java.util.List;

/**
 * 거래일 목록 응답.
 *
 * @param latestDate  최근 거래일
 * @param tradingDays 거래일 목록
 */
public record TradingDaysResponse(String latestDate, List<String> tradingDays) {}
