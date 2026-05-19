package com.dove.api.stock.listing.dto;

import java.util.Map;

public record IndicatorBar(String date, Map<String, Double> values) {}
