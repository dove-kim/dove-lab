package com.dove.api.stock.market.dto;

import java.util.List;

public record TradingDaysResponse(String latestDate, List<String> tradingDays) {}
