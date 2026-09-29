package com.dove.stockcollection.application.service;

import com.dove.concurrent.Parallel;
import com.dove.concurrent.ParallelException;
import com.dove.indicator.application.service.IndicatorCursorService;
import com.dove.stockcollection.application.port.DailyPriceFetcher;
import com.dove.stockcollection.domain.model.DailyCandle;
import com.dove.stock.application.service.StockPriceCommandService;
import com.dove.stock.application.service.StockPriceQueryService;
import com.dove.stock.application.service.StockQueryService;
import com.dove.stock.domain.entity.StockPrice;
import com.dove.stock.domain.enums.PriceType;
import com.dove.stock.domain.enums.StockExchange;
import com.dove.stockcollection.application.dto.CollectionUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
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
@ConditionalOnBean(DailyPriceFetcher.class)
@RequiredArgsConstructor
public class PriceCollectionService {

    @Value("${collection.concurrency:40}")
    private int concurrency;

    private final DailyPriceFetcher fetcher;
    private final StockQueryService stockQueryService;
    private final StockPriceCommandService priceCommandService;
    private final StockPriceQueryService priceQueryService;
    private final IndicatorCursorService cursorService;

    /** 가격제한폭(±30%)에 여유를 둔 비율. 이보다 큰 수정주가 변동은 시장 변동이 아니라 미반영 권리이벤트로 본다. */
    private static final double PRICE_LIMIT_RATIO = 0.35;

    /**
     * KIS 일봉 변동구분코드가 수정주가 이벤트(배당락·분할 등)인지 여부.
     */
    private static boolean isAdjustmentEvent(String kisCode) {
        return kisCode != null && !kisCode.isBlank() && !"00".equals(kisCode);
    }

    /**
     * 수정주가가 가격제한폭을 넘게 움직였는지 여부. 액면변경·감자는 KIS 락 구분 코드에 잡히지 않으므로
     * 가격 자체로 감지한다.
     */
    private static boolean exceedsPriceLimit(long prevClose, long closePrice) {
        return prevClose > 0 && Math.abs(closePrice - prevClose) > prevClose * PRICE_LIMIT_RATIO;
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

        // 병렬 수집·저장 (청크 단위로 즉시 저장 → 메모리 = 청크 1개치).
        // 한 종목 실패는 건너뛰고 계속, 실패가 임계(10%·최소 20)에 달하면 체계적 장애로 보고 중단.
        int maxFailures = Math.max(20, units.size() / 10);
        List<CollectionUnit> failedUnits = Parallel.runResilient(units, concurrency, maxFailures, unit -> {
            // ADJUSTED는 직전 저장 종가에서 이어 붙여 가격 점프를 판단한다 (청크 경계에서도 끊기지 않게 유지)
            long[] prevClose = {unit.priceType() == PriceType.ADJUSTED ? lastStoredClose(unit, exchange) : 0L};
            fetcher.fetchInWindows(exchange, unit.ticker(), unit.from(), unit.to(), unit.priceType(),
                    chunk -> {
                        List<StockPrice> prices = new ArrayList<>(chunk.size());
                        for (DailyCandle c : chunk) {
                            if (!c.hasValidPrices()) continue; // 외부 응답의 음수·0 가격은 저장하지 않음
                            prices.add(toPrice(unit.ticker(), exchange, unit.priceType(), c));
                            // 수정주가 이벤트 감지 → ADJUSTED 재조회 트리거
                            if (unit.priceType() == PriceType.RAW && isAdjustmentEvent(c.adjustmentCode())) {
                                adjEventTickers.add(unit.ticker());
                            } else if (unit.priceType() == PriceType.ADJUSTED) {
                                if (exceedsPriceLimit(prevClose[0], c.closePrice())) {
                                    adjEventTickers.add(unit.ticker());
                                }
                                prevClose[0] = c.closePrice();
                            }
                        }
                        priceCommandService.upsertAll(prices);
                    });
            progress.onProgress(done.incrementAndGet());
        });
        if (!failedUnits.isEmpty()) {
            log.warn("[{}] {}작업 중 {}건 실패(건너뜀): {}", exchange, units.size(), failedUnits.size(),
                    failedUnits.stream().map(CollectionUnit::ticker).distinct().limit(10).toList());
        }

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
        int maxFailures = Math.max(20, tickers.size() / 10);
        List<String> failed = Parallel.runResilient(tickers, concurrency, maxFailures, ticker -> {
            fetcher.fetchAdjustedBackward(exchange, ticker, from, upTo, (List<DailyCandle> chunk) -> {
                List<StockPrice> prices = chunk.stream()
                        .filter(DailyCandle::hasValidPrices)
                        .map(c -> toPrice(ticker, exchange, PriceType.ADJUSTED, c))
                        .toList();
                priceCommandService.upsertAll(prices);
            });
            cursorService.clearAdjusted(ticker, exchange);
            progress.onAdjustedProgress(adjDone.incrementAndGet());
        });
        if (!failed.isEmpty()) {
            log.warn("[{}] ADJUSTED 재조회 {}종목 중 {}건 실패(건너뜀): {}", exchange, tickers.size(), failed.size(),
                    failed.stream().distinct().limit(10).toList());
        }
    }

    /**
     * 수집 구간 직전에 저장돼 있던 수정주가 종가. 없으면 0.
     */
    private long lastStoredClose(CollectionUnit unit, StockExchange exchange) {
        return priceQueryService.findBefore(unit.ticker(), exchange, PriceType.ADJUSTED, unit.from(), 1)
                .stream()
                .map(StockPrice::getClosePrice)
                .findFirst()
                .orElse(0L);
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
