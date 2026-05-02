package com.dove.stockconsumer.service;

import com.dove.market.domain.enums.MarketType;
import com.dove.stockprice.application.service.DailyStockPriceCommandService;
import com.dove.stockprice.domain.entity.DailyStockPrice;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class DailyStockPriceSaveService {
    private final DailyStockPriceCommandService dailyStockPriceCommandService;

    @Transactional
    public void save(
            LocalDate baseDate, MarketType marketType, String stockCode,
            Long volume, Long openingPrice, Long closingPrice, Long lowPrice, Long highPrice
    ) {
        if (!isValid(baseDate, marketType, stockCode, volume, openingPrice, closingPrice, lowPrice, highPrice)) {
            return;
        }
        dailyStockPriceCommandService.save(new DailyStockPrice(
                marketType, stockCode, baseDate, volume,
                openingPrice, closingPrice, lowPrice, highPrice
        ));
    }

    private boolean isValid(
            LocalDate baseDate, MarketType marketType, String stockCode,
            Long volume, Long openingPrice, Long closingPrice, Long lowPrice, Long highPrice
    ) {
        List<String> missing = new ArrayList<>();
        if (baseDate == null) missing.add("baseDate");
        if (marketType == null) missing.add("marketType");
        if (stockCode == null || stockCode.isEmpty()) missing.add("stockCode");
        if (volume == null) missing.add("volume");
        if (openingPrice == null) missing.add("openingPrice");
        if (closingPrice == null) missing.add("closingPrice");
        if (lowPrice == null) missing.add("lowPrice");
        if (highPrice == null) missing.add("highPrice");

        if (!missing.isEmpty()) {
            log.error("필수 파라미터 누락으로 데이터 저장을 건너뜁니다. 누락된 파라미터: {}", String.join(", ", missing));
            return false;
        }
        return true;
    }
}
