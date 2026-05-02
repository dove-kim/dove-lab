package com.dove.technicalindicator.domain.service;

import com.dove.market.domain.enums.MarketType;
import com.dove.stock.application.service.StockListedDateQueryService;
import com.dove.stockprice.application.service.DailyStockPriceQueryService;
import com.dove.technicalindicator.domain.enums.DateState;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class IndicatorDateClassifier {

    private final DailyStockPriceQueryService dailyStockPriceQueryService;
    private final StockListedDateQueryService stockListedDateQueryService;

    @Transactional(readOnly = true)
    public DateState classify(MarketType market, String code, LocalDate date) {
        if (dailyStockPriceQueryService.existsByMarketAndCodeAndDate(market, code, date)) {
            return DateState.ACTIVE;
        }
        if (stockListedDateQueryService.existsByMarketAndCodeAndDate(market, code, date)) {
            return DateState.SUSPENDED;
        }
        if (stockListedDateQueryService.existsByMarketAndDate(market, date)) {
            return DateState.DELISTED;
        }
        return DateState.LISTING_NOT_SYNCED;
    }
}

