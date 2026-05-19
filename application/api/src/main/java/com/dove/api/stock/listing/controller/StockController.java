package com.dove.api.stock.listing.controller;

import com.dove.api.stock.listing.dto.IndicatorBar;
import com.dove.api.stock.listing.dto.PriceBar;
import com.dove.api.stock.listing.dto.StockResponse;
import com.dove.api.stock.listing.service.StockChartService;
import com.dove.indicator.domain.enums.IndicatorType;
import com.dove.market.domain.enums.MarketType;
import com.dove.stock.application.service.StockQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Objects;

@RestController
@RequestMapping("/stocks")
@RequiredArgsConstructor
public class StockController {

    private final StockQueryService stockQueryService;
    private final StockChartService stockChartService;

    @GetMapping
    public List<StockResponse> getStocks() {
        return stockQueryService.findAll()
                .stream()
                .map(StockResponse::from)
                .toList();
    }

    @GetMapping("/{code}/prices")
    public List<PriceBar> getPrices(
            @PathVariable String code,
            @RequestParam String market,
            @RequestParam(defaultValue = "60") int limit) {
        return stockChartService.getPriceBars(parseMarket(market), code, limit);
    }

    @GetMapping("/{code}/indicators")
    public List<IndicatorBar> getIndicators(
            @PathVariable String code,
            @RequestParam String market,
            @RequestParam(defaultValue = "120") int limit,
            @RequestParam List<String> types) {
        List<IndicatorType> indicatorTypes = types.stream()
                .map(t -> {
                    try { return IndicatorType.valueOf(t); }
                    catch (IllegalArgumentException e) { return null; }
                })
                .filter(Objects::nonNull)
                .toList();
        return stockChartService.getIndicatorBars(parseMarket(market), code, limit, indicatorTypes);
    }

    private MarketType parseMarket(String market) {
        try {
            return MarketType.valueOf(market.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "INVALID_MARKET");
        }
    }
}
