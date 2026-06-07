package com.dove.stockcollection.application.service;

import com.dove.concurrent.Parallel;
import com.dove.concurrent.ParallelException;
import com.dove.indicator.application.service.IndicatorCursorService;
import com.dove.stockcollection.application.port.DailyPriceFetcher;
import com.dove.stockcollection.domain.model.DailyCandle;
import com.dove.stock.application.service.StockPriceCommandService;
import com.dove.stock.application.service.StockQueryService;
import com.dove.stock.domain.entity.StockPrice;
import com.dove.stock.domain.enums.PriceType;
import com.dove.stock.domain.enums.StockExchange;
import com.dove.stockcollection.application.dto.CollectionUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * KIS 일봉 주가 수집 코어. 주가 수집·저장과 지표 커서 조정을 담당하며, 지표 계산은 하지 않는다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PriceCollectionService {

    @Value("${collection.concurrency:40}")
    private int concurrency;

    private final DailyPriceFetcher fetcher;
    private final StockQueryService stockQueryService;
    private final StockPriceCommandService priceCommandService;
    private final IndicatorCursorService cursorService;

    /**
     * KIS 일봉 변동구분코드가 수정주가 이벤트(배당락·분할 등)인지 여부.
     */
    private static boolean isAdjustmentEvent(String kisCode) {
        return kisCode != null && !kisCode.isBlank() && !"00".equals(kisCode);
    }

    /**
     * 거래소·기간 주가를 수집한다.
     *
     * @throws ParallelException 수집 도중 KIS 호출이 실패한 경우
     */
    public void collect(StockExchange exchange, LocalDate from, LocalDate to, CollectionProgress progress,
                        LocalDate adjustedFrom)
            throws ParallelException {
        List<String> tickers = stockQueryService.findTickersByExchange(exchange);
        if (tickers.isEmpty()) {
            log.info("[{}] 대상 종목 없음", exchange);
            progress.onTotal(0);
            return;
        }

        List<CollectionUnit> units = buildUnits(tickers, from, to);
        progress.onTotal(units.size());
        log.info("[{}] 주가 수집 시작: {}종목 × {}가격유형 = {}작업 / {}~{}",
                exchange, tickers.size(), PriceType.values().length, units.size(), from, to);

        Set<String> adjEventTickers = ConcurrentHashMap.newKeySet();
        AtomicInteger done = new AtomicInteger();

        // 병렬 수집·저장 (청크 단위로 즉시 저장 → 메모리 = 청크 1개치)
        Parallel.run(units, concurrency, unit -> {
            fetcher.fetchInWindows(exchange, unit.ticker(), unit.from(), unit.to(), unit.priceType(),
                    chunk -> {
                        List<StockPrice> prices = new ArrayList<>(chunk.size());
                        for (DailyCandle c : chunk) {
                            prices.add(toPrice(unit.ticker(), exchange, unit.priceType(), c));
                            // 수정주가 이벤트 감지 → ADJUSTED 재조회 트리거
                            if (unit.priceType() == PriceType.RAW && isAdjustmentEvent(c.adjustmentCode())) {
                                adjEventTickers.add(unit.ticker());
                            }
                        }
                        priceCommandService.upsertAll(prices);
                    });
            progress.onProgress(done.incrementAndGet());
        });

        // 재수집 구간의 지표 커서를 from 직전으로 일괄 되돌림 (거래소 전체 1문장, RAW·ADJUSTED 동시)
        cursorService.rewindExchangeBefore(exchange, from);

        // 신규 수정주가 이벤트 종목은 ADJUSTED 재조회 (adjustedFrom~to). adjustedFrom=null이면 스킵.
        if (adjustedFrom != null && !adjEventTickers.isEmpty()) {
            log.info("[{}] 신규 수정주가 이벤트 {}종목 → ADJUSTED 재조회 ({}~)", exchange, adjEventTickers.size(), adjustedFrom);
            progress.onAdjustedTotal(adjEventTickers.size()); // 메인 total과 별개로 추적
            refetchAdjusted(exchange, adjEventTickers, adjustedFrom, to, progress);
        } else if (!adjEventTickers.isEmpty()) {
            log.info("[{}] 수정주가 이벤트 {}종목 감지(기록만) — 재조회 생략", exchange, adjEventTickers.size());
        }

        log.info("[{}] 주가 수집 완료", exchange);
    }

    /**
     * 수정주가 이벤트 종목의 ADJUSTED 전체를 역방향 페이징으로 재수집한다.
     */
    private void refetchAdjusted(StockExchange exchange, Set<String> tickers, LocalDate from, LocalDate upTo,
                                 CollectionProgress progress) {
        AtomicInteger adjDone = new AtomicInteger();
        Parallel.run(tickers, concurrency, ticker -> {
            fetcher.fetchAdjustedBackward(exchange, ticker, from, upTo, (List<DailyCandle> chunk) -> {
                List<StockPrice> prices = chunk.stream()
                        .map(c -> toPrice(ticker, exchange, PriceType.ADJUSTED, c))
                        .toList();
                priceCommandService.upsertAll(prices);
            });
            cursorService.clearAdjusted(ticker, exchange);
            progress.onAdjustedProgress(adjDone.incrementAndGet());
        });
    }

    private StockPrice toPrice(String ticker, StockExchange exchange, PriceType type, DailyCandle c) {
        return new StockPrice(ticker, exchange, type, c.tradingDate(),
                c.openPrice(), c.highPrice(), c.lowPrice(), c.closePrice(),
                c.accumulatedVolume(), c.accumulatedTurnover());
    }

    /**
     * 종목 × 가격유형 조합으로 작업 단위 목록을 만든다.
     */
    private List<CollectionUnit> buildUnits(List<String> tickers, LocalDate from, LocalDate to) {
        List<CollectionUnit> units = new ArrayList<>(tickers.size() * PriceType.values().length);
        for (String ticker : tickers) {
            for (PriceType priceType : PriceType.values()) {
                units.add(new CollectionUnit(ticker, priceType, from, to));
            }
        }
        return units;
    }
}
