package com.dove.api.stock.market.controller;

import com.dove.api.stock.market.dto.TradingDaysResponse;
import com.dove.market.domain.enums.MarketType;
import com.dove.stock.application.service.DailyStockPriceQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/market")
@RequiredArgsConstructor
public class MarketController {

    private static final List<MarketType> ALL_MARKETS =
            List.of(MarketType.KOSPI, MarketType.KOSDAQ, MarketType.KONEX);

    private final DailyStockPriceQueryService priceQueryService;

    @GetMapping("/trading-days")
    public TradingDaysResponse getTradingDays(
            @RequestParam(defaultValue = "90") int limit) {
        LocalDate today = LocalDate.now();
        List<String> dates = priceQueryService
                .findRecentTradeDates(ALL_MARKETS, today, limit)
                .stream()
                .map(LocalDate::toString)
                .toList();
        String latestDate = dates.isEmpty() ? today.toString() : dates.get(0);
        return new TradingDaysResponse(latestDate, dates);
    }
}
