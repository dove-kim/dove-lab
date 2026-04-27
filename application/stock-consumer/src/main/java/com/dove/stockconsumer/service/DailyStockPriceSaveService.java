package com.dove.stockconsumer.service;

import com.dove.market.domain.enums.MarketType;
import com.dove.stock.application.service.StockCommandService;
import com.dove.stock.application.service.StockQueryService;
import com.dove.stock.domain.entity.Stock;
import com.dove.stock.domain.enums.TradingStatus;
import com.dove.stockprice.application.service.DailyStockPriceCommandService;
import com.dove.stockprice.domain.entity.DailyStockPrice;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * 종목 마스터 및 일별 주가 데이터를 저장/업데이트하는 조합 유스케이스.
 * 필수 파라미터 유효성 검사 후 저장하며, 누락 시 건너뛴다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DailyStockPriceSaveService {
    private final StockQueryService stockQueryService;
    private final StockCommandService stockCommandService;
    private final DailyStockPriceCommandService dailyStockPriceCommandService;

    @Transactional
    public void update(
            LocalDate baseDate, MarketType marketType,
            String stockCode, String stockName,
            Long volume,
            Long openingPrice, Long closingPrice,
            Long lowPrice, Long highPrice
    ) {
        if (!validateInputParameters(baseDate, marketType, stockCode, stockName,
                volume, openingPrice, closingPrice, lowPrice, highPrice)) {
            return;
        }

        Stock stock = stockQueryService.findByMarketAndCode(marketType, stockCode)
                .map(existingStock -> existingStock.updateName(stockName))
                .orElseGet(() -> new Stock(marketType, stockCode, stockName, TradingStatus.ACTIVE));
        stockCommandService.save(stock);

        dailyStockPriceCommandService.save(new DailyStockPrice(
                marketType, stockCode, baseDate, volume,
                openingPrice, closingPrice, lowPrice, highPrice
        ));
    }

    private boolean validateInputParameters(
            LocalDate baseDate, MarketType marketType,
            String stockCode, String stockName,
            Long volume,
            Long openingPrice, Long closingPrice,
            Long lowPrice, Long highPrice
    ) {
        List<String> missingParams = new ArrayList<>();

        if (baseDate == null) missingParams.add("baseDate");
        if (marketType == null) missingParams.add("marketType");
        if (stockCode == null || stockCode.isEmpty()) missingParams.add("stockCode");
        if (stockName == null || stockName.isEmpty()) missingParams.add("stockName");
        if (volume == null) missingParams.add("volume");
        if (openingPrice == null) missingParams.add("openingPrice");
        if (closingPrice == null) missingParams.add("closingPrice");
        if (lowPrice == null) missingParams.add("lowPrice");
        if (highPrice == null) missingParams.add("highPrice");

        if (!missingParams.isEmpty()) {
            log.error("필수 파라미터 누락으로 데이터 저장을 건너뜁니다. 누락된 파라미터: {}",
                    String.join(", ", missingParams));
            return false;
        }
        return true;
    }
}
