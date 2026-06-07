package com.dove.api.search.market.controller;

import com.dove.api.search.market.dto.TradingDaysResponse;
import com.dove.market.domain.enums.MarketType;
import com.dove.stock.application.service.StockPriceQueryService;
import com.dove.stock.domain.enums.PriceType;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/**
 * 시장 거래일 조회 API.
 */
@RestController
@RequestMapping("/market")
@RequiredArgsConstructor
public class MarketController {

    private static final List<MarketType> ALL_MARKETS = MarketType.KRX_MARKETS;

    private final StockPriceQueryService priceQueryService;

    /**
     * 최근 거래일 목록과 최신 거래일을 반환한다.
     */
    @GetMapping("/trading-days")
    public TradingDaysResponse getTradingDays(@RequestParam(defaultValue = "90") int limit) {
        LocalDate today = LocalDate.now();
        List<String> dates = priceQueryService
                .findRecentTradeDates(ALL_MARKETS, PriceType.RAW, today, limit)
                .stream()
                .map(LocalDate::toString)
                .toList();
        String latestDate = dates.isEmpty() ? today.toString() : dates.get(0);
        return new TradingDaysResponse(latestDate, dates);
    }
}
