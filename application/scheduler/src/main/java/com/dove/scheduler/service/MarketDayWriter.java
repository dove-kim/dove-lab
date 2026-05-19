package com.dove.scheduler.service;

import com.dove.market.application.service.MarketTradingDateService;
import com.dove.market.domain.enums.MarketType;
import com.dove.stock.application.service.StockCommandService;
import com.dove.stock.application.service.StockQueryService;
import com.dove.stock.domain.entity.Stock;
import com.dove.stock.application.service.DailyStockPriceCommandService;
import com.dove.stock.application.service.DailyStockPriceQueryService;
import com.dove.stock.domain.entity.DailyStockPrice;
import com.dove.krx.DailyMarketData;
import com.dove.krx.StockListing;
import com.dove.krx.StockPrice;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 하루치 시장 데이터를 단일 트랜잭션으로 저장한다.
 *
 * <p>OPEN: 종목·주가 upsert + MTD=true / CLOSED: MTD=false / UNCONFIRMED: no-op
 *
 * <p>여러 도메인(stock, market)에 걸친 저장을 하나의 트랜잭션으로 묶는
 * 애플리케이션 계층 조합 책임이다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MarketDayWriter {

    private final StockQueryService stockQueryService;
    private final StockCommandService stockCommandService;
    private final DailyStockPriceQueryService dailyStockPriceQueryService;
    private final DailyStockPriceCommandService dailyStockPriceCommandService;
    private final MarketTradingDateService marketTradingDateService;

    @Transactional
    public void write(MarketType market, DailyMarketData day) {
        switch (day.status()) {
            case OPEN -> {
                if (dailyStockPriceQueryService.existsByMarketAndDate(market, day.date())) {
                    log.info("[{}] 이미 동기화됨, 스킵: {}", market, day.date());
                    return;
                }
                upsertStocks(market, day.listings());
                savePrices(market, day.date(), day.prices());
                marketTradingDateService.upsert(market, day.date(), true);
                log.info("[{}] OPEN 저장 완료: {}", market, day.date());
            }
            case CLOSED -> {
                marketTradingDateService.upsert(market, day.date(), false);
                log.info("[{}] CLOSED 기록: {}", market, day.date());
            }
            case UNCONFIRMED -> log.debug("[{}] UNCONFIRMED — 스킵: {}", market, day.date());
        }
    }

    private void upsertStocks(MarketType market, List<StockListing> listings) {
        Map<String, Stock> existing = stockQueryService.findAllByMarket(market)
                .stream().collect(Collectors.toMap(s -> s.getId().getCode(), s -> s));

        List<Stock> toInsert = new ArrayList<>();
        for (StockListing listing : listings) {
            Stock stock = existing.get(listing.ticker());
            if (stock == null) {
                toInsert.add(new Stock(market, listing.ticker(), listing.name(),
                        listing.listingDate(), listing.isin()));
            } else if (!listing.name().equals(stock.getName())) {
                stock.updateName(listing.name());
            }
        }
        if (!toInsert.isEmpty()) {
            stockCommandService.saveAll(toInsert);
            log.info("[{}] 신규 종목 {}건 등록", market, toInsert.size());
        }
    }

    private void savePrices(MarketType market, LocalDate date, List<StockPrice> prices) {
        List<DailyStockPrice> entities = prices.stream()
                .map(p -> new DailyStockPrice(market, p.stockCode(), date,
                        p.tradingVolume(), p.openingPrice(), p.closingPrice(),
                        p.lowestPrice(), p.highestPrice()))
                .toList();
        dailyStockPriceCommandService.saveAll(entities);
    }
}
