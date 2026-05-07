package com.dove.stockapi.domain.stock;

import com.dove.market.domain.enums.MarketType;
import com.dove.stock.application.service.StockQueryService;
import com.dove.stock.domain.enums.TradingStatus;
import com.dove.stockapi.domain.stock.dto.StockResponse;
import com.dove.stockprice.application.service.DailyStockPriceQueryService;
import com.dove.stockprice.domain.entity.DailyStockPrice;
import com.dove.technicalindicator.application.service.TechnicalIndicatorQueryService;
import com.dove.technicalindicator.domain.enums.IndicatorType;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/stocks")
@RequiredArgsConstructor
public class StockController {

    private final StockQueryService stockQueryService;
    private final DailyStockPriceQueryService priceQueryService;
    private final TechnicalIndicatorQueryService indicatorQueryService;

    @GetMapping
    public List<StockResponse> getActiveStocks() {
        return stockQueryService.findAllByStatus(TradingStatus.ACTIVE)
                .stream()
                .map(StockResponse::from)
                .toList();
    }

    @GetMapping("/{code}/prices")
    public List<PriceBar> getPrices(
            @PathVariable String code,
            @RequestParam String market,
            @RequestParam(defaultValue = "60") int limit) {
        MarketType marketType;
        try {
            marketType = MarketType.valueOf(market.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "INVALID_MARKET");
        }
        return priceQueryService
                .findRecentDailyStockPrice(marketType, code, LocalDate.now(), limit)
                .stream()
                .sorted(Comparator.comparing(p -> p.getId().getTradeDate()))
                .map(PriceBar::from)
                .toList();
    }

    @GetMapping("/{code}/indicators")
    public List<IndicatorBar> getIndicators(
            @PathVariable String code,
            @RequestParam String market,
            @RequestParam(defaultValue = "120") int limit,
            @RequestParam List<String> types) {
        MarketType marketType;
        try {
            marketType = MarketType.valueOf(market.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "INVALID_MARKET");
        }
        List<IndicatorType> indicatorTypes = types.stream()
                .map(t -> {
                    try { return IndicatorType.valueOf(t); }
                    catch (IllegalArgumentException e) { return null; }
                })
                .filter(t -> t != null)
                .collect(Collectors.toList());
        if (indicatorTypes.isEmpty()) return List.of();

        Map<LocalDate, Map<IndicatorType, Double>> raw =
                indicatorQueryService.findRecentByStock(marketType, code, indicatorTypes, limit);

        return raw.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(e -> new IndicatorBar(
                        e.getKey().toString(),
                        e.getValue().entrySet().stream()
                                .collect(Collectors.toMap(
                                        kv -> kv.getKey().name(),
                                        Map.Entry::getValue
                                ))
                ))
                .collect(Collectors.toList());
    }

    public record PriceBar(String date, long open, long high, long low, long close, long volume) {
        static PriceBar from(DailyStockPrice p) {
            return new PriceBar(
                    p.getId().getTradeDate().toString(),
                    p.getOpenPrice(), p.getHighPrice(),
                    p.getLowPrice(), p.getClosePrice(),
                    p.getVolume()
            );
        }
    }

    public record IndicatorBar(String date, Map<String, Double> values) {}
}
