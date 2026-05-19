package com.dove.api.stock.listing.service;

import com.dove.api.stock.listing.dto.IndicatorBar;
import com.dove.api.stock.listing.dto.PriceBar;
import com.dove.indicator.application.service.TechnicalIndicatorQueryService;
import com.dove.indicator.domain.enums.IndicatorType;
import com.dove.market.application.service.MarketTradingDateService;
import com.dove.market.domain.enums.MarketType;
import com.dove.stock.application.service.DailyStockPriceQueryService;
import com.dove.stock.domain.entity.DailyStockPrice;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StockChartService {

    private final DailyStockPriceQueryService priceQueryService;
    private final MarketTradingDateService marketTradingDateService;
    private final TechnicalIndicatorQueryService indicatorQueryService;

    /**
     * 최근 limit 개 개장일의 주가 봉 데이터를 조회한다.
     */
    public List<PriceBar> getPriceBars(MarketType market, String code, int limit) {
        LocalDate to = LocalDate.now();
        LocalDate from = to.minusDays((long) limit * 5);
        Map<LocalDate, Boolean> statuses = marketTradingDateService.findStatusesInRange(market, from, to);

        List<LocalDate> openDates = statuses.entrySet().stream()
                .filter(Map.Entry::getValue)
                .map(Map.Entry::getKey)
                .sorted(Comparator.reverseOrder())
                .limit(limit)
                .sorted()
                .toList();

        if (openDates.isEmpty()) return List.of();

        Map<LocalDate, DailyStockPrice> prices =
                priceQueryService.findByMarketAndCodeAndDates(market, code, openDates);

        return openDates.stream()
                .map(date -> PriceBar.of(date, prices.get(date)))
                .toList();
    }

    /**
     * 최근 limit 개 거래일의 기술적 지표 데이터를 조회한다.
     */
    public List<IndicatorBar> getIndicatorBars(MarketType market, String code, int limit, List<IndicatorType> types) {
        if (types.isEmpty()) return List.of();

        Map<LocalDate, Map<IndicatorType, Double>> raw =
                indicatorQueryService.findRecentByStock(market, code, types, limit);

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
}
