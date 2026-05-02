package com.dove.stockconsumer.service;

import com.dove.eventretry.application.service.FailedEventCommandService;
import com.dove.market.domain.enums.MarketType;
import com.dove.stock.application.port.StockListingFetcher;
import com.dove.stock.application.service.StockCommandService;
import com.dove.stock.application.service.StockListedDateCommandService;
import com.dove.stock.application.service.StockListedDateQueryService;
import com.dove.stock.application.service.StockQueryService;
import com.dove.stock.domain.entity.Stock;
import com.dove.stock.domain.entity.StockListedDate;
import com.dove.stock.domain.entity.StockListedDateId;
import com.dove.stock.domain.enums.TradingStatus;
import com.dove.market.application.dto.DailyMarketDataQuery;
import com.dove.market.application.service.MarketDataCursorCommandService;
import com.dove.market.application.service.MarketTradingDateCommandService;
import com.dove.stockprice.application.port.DailyPriceFetcher;
import com.dove.stockprice.application.port.StockInfo;
import com.dove.technicalindicator.application.dto.IndicatorCalcTrigger;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Clock;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DailyMarketDataService {

    private final StockListingFetcher stockListingFetcher;
    private final DailyPriceFetcher dailyPriceFetcher;
    private final StockQueryService stockQueryService;
    private final StockCommandService stockCommandService;
    private final StockListedDateQueryService stockListedDateQueryService;
    private final StockListedDateCommandService stockListedDateCommandService;
    private final DailyStockPriceSaveService dailyStockPriceSaveService;
    private final MarketDataCursorCommandService marketDataCursorCommandService;
    private final MarketTradingDateCommandService marketTradingDateCommandService;
    private final FailedEventCommandService failedEventCommandService;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    @Transactional
    public void process(MarketType market, LocalDate date) {
        Optional<Map<String, String>> listingResult = fetchListing(market, date);
        if (listingResult.isEmpty()) return;

        Optional<List<StockInfo>> priceResult = fetchPrice(market, date);
        if (priceResult.isEmpty()) return;

        Map<String, String> listingStocks = listingResult.get();
        List<StockInfo> prices = priceResult.get();
        boolean isOpen = !prices.isEmpty();

        if (!listingStocks.isEmpty()) {
            insertNewStocks(market, listingStocks);
            insertListedDates(market, date, listingStocks);
        }
        for (StockInfo info : prices) {
            dailyStockPriceSaveService.save(
                    info.tradingDate(), info.marketType(), info.stockCode(),
                    info.tradingVolume(), info.openingPrice(), info.closingPrice(),
                    info.lowestPrice(), info.highestPrice());
        }

        marketDataCursorCommandService.upsert(market, date);
        marketTradingDateCommandService.upsert(market, date, isOpen);

        if (isOpen) {
            Set<String> listedCodes = new HashSet<>(listingStocks.keySet());
            Set<String> tradedCodes = prices.stream()
                    .map(StockInfo::stockCode).collect(Collectors.toSet());
            reconcileAndGetChanged(market, listedCodes, tradedCodes);
            tradedCodes.forEach(code -> publishIndicatorCalcAfterCommit(market, code, date));
        }
    }

    /** RetryLater → 예외 throw. PermanentFail → escalate + cursor 전진 후 empty. */
    private Optional<Map<String, String>> fetchListing(MarketType market, LocalDate date) {
        return switch (stockListingFetcher.fetch(market, date)) {
            case StockListingFetcher.Outcome.Success s -> Optional.of(s.stocks());
            case StockListingFetcher.Outcome.Holiday h -> Optional.of(Map.of());
            case StockListingFetcher.Outcome.RetryLater r -> {
                throw new IllegalStateException("RetryLater: " + r.detail());
            }
            case StockListingFetcher.Outcome.PermanentFail p -> {
                log.error("종목 조회 영구 실패: market={}, date={}, reason={}", market, date, p.reason());
                failedEventCommandService.escalate(
                        DailyMarketDataQuery.TOPIC, eventKey(market, date),
                        serializeQuery(market, date), p.reason().name(), p.detail(), clock.instant());
                marketDataCursorCommandService.upsert(market, date);
                yield Optional.empty();
            }
        };
    }

    /** RetryLater → 예외 throw. PermanentFail → escalate + cursor 전진 후 empty. */
    private Optional<List<StockInfo>> fetchPrice(MarketType market, LocalDate date) {
        return switch (dailyPriceFetcher.fetchDailyMarketData(market, date)) {
            case DailyPriceFetcher.Outcome.Success s -> Optional.of(s.stocks());
            case DailyPriceFetcher.Outcome.Holiday h -> Optional.of(List.of());
            case DailyPriceFetcher.Outcome.RetryLater r -> {
                throw new IllegalStateException("RetryLater: " + r.detail());
            }
            case DailyPriceFetcher.Outcome.PermanentFail p -> {
                log.error("주가 조회 영구 실패: market={}, date={}, reason={}", market, date, p.reason());
                failedEventCommandService.escalate(
                        DailyMarketDataQuery.TOPIC, eventKey(market, date),
                        serializeQuery(market, date), p.reason().name(), p.detail(), clock.instant());
                marketDataCursorCommandService.upsert(market, date);
                yield Optional.empty();
            }
        };
    }

    private void insertNewStocks(MarketType market, Map<String, String> listingStocks) {
        Set<String> existing = stockQueryService.findAllByMarket(market).stream()
                .map(s -> s.getId().getCode())
                .collect(Collectors.toSet());
        List<Stock> toInsert = listingStocks.entrySet().stream()
                .filter(e -> !existing.contains(e.getKey()))
                .map(e -> new Stock(market, e.getKey(), e.getValue(), TradingStatus.ACTIVE))
                .toList();
        if (!toInsert.isEmpty()) {
            stockCommandService.saveAll(toInsert);
        }
    }

    private Set<String> insertListedDates(MarketType market, LocalDate date, Map<String, String> listingStocks) {
        Set<String> existing = stockListedDateQueryService.findAllByMarketAndDate(market, date).stream()
                .map(r -> r.getId().getCode())
                .collect(Collectors.toSet());
        List<StockListedDate> toInsert = listingStocks.keySet().stream()
                .filter(code -> !existing.contains(code))
                .map(code -> new StockListedDate(new StockListedDateId(market, code, date)))
                .toList();
        if (!toInsert.isEmpty()) {
            stockListedDateCommandService.saveAll(toInsert);
        }
        return toInsert.stream().map(s -> s.getId().getCode()).collect(Collectors.toSet());
    }

    private void reconcileAndGetChanged(MarketType market, Set<String> listedCodes, Set<String> tradedCodes) {
        List<Stock> stocks = stockQueryService.findAllByMarket(market);
        for (Stock stock : stocks) {
            TradingStatus next = resolveStatus(stock.getId().getCode(), listedCodes, tradedCodes);
            if (stock.getTradingStatus() != next) {
                stock.updateTradingStatus(next);
            }
        }
    }

    private TradingStatus resolveStatus(String code, Set<String> listedCodes, Set<String> tradedCodes) {
        if (!listedCodes.contains(code)) return TradingStatus.DELISTED;
        return tradedCodes.contains(code) ? TradingStatus.ACTIVE : TradingStatus.SUSPENDED;
    }

    private void publishIndicatorCalcAfterCommit(MarketType market, String code, LocalDate date) {
        Runnable publish = () -> {
            IndicatorCalcTrigger trigger = new IndicatorCalcTrigger(market, code, date);
            kafkaTemplate.send(IndicatorCalcTrigger.TOPIC, trigger.messageKey(), trigger);
        };
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() { publish.run(); }
            });
        } else {
            publish.run();
        }
    }

    private String eventKey(MarketType market, LocalDate date) {
        return market + "-" + date;
    }

    private String serializeQuery(MarketType market, LocalDate date) {
        try {
            return objectMapper.writeValueAsString(new DailyMarketDataQuery(market, date));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("DailyMarketDataQuery 직렬화 실패", e);
        }
    }
}
