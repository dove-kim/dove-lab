package com.dove.krx.infrastructure.adapter;

import com.dove.krx.DailyMarketData;
import com.dove.krx.DayStatus;
import com.dove.krx.StockListing;
import com.dove.krx.StockPrice;
import com.dove.krx.acl.KrxDailyStockPriceTranslator;
import com.dove.krx.acl.KrxListedStockTranslator;
import com.dove.krx.infrastructure.client.KrxDailyPriceResponse;
import com.dove.krx.infrastructure.client.KrxListedStockResponse;
import com.dove.krx.infrastructure.client.KrxStockClient;
import com.dove.market.domain.enums.MarketType;
import feign.FeignException;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.stream.IntStream;

/**
 * KRX Open API 기반 날짜 구간 조회.
 *
 * <p>날짜별로 가상 스레드를 사용해 병렬 조회하며, Semaphore로 동시 요청 수를 제한한다.
 * PENDING 후처리를 통해 CLOSED/UNCONFIRMED를 확정한다.
 *
 * <p>판별 규칙:
 * <ol>
 *   <li>종목 있음 + 주가 있음 → OPEN</li>
 *   <li>종목 없음 + 주가 없음 → UNCONFIRMED</li>
 *   <li>종목 있음 + 주가 없음 → PENDING (후처리)</li>
 * </ol>
 * PENDING 후처리: 구간 내 OPEN이 하나 이상이면 lastOpenDate 이전 PENDING → CLOSED,
 * lastOpenDate 이후 PENDING → UNCONFIRMED. OPEN이 없으면 전부 UNCONFIRMED.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class KrxTradingDayFetcher {

    @Value("${krx.api.auth-key}")
    private String krxApiAuthKey;

    @Value("${krx.fetch-concurrency:5}")
    private int fetchConcurrency;

    // 테스트 환경(Spring 컨텍스트 없음)을 위한 기본값; @PostConstruct에서 설정값으로 교체
    private Semaphore semaphore = new Semaphore(5);

    @PostConstruct
    void init() {
        this.semaphore = new Semaphore(fetchConcurrency);
    }

    private final KrxStockClient krxStockClient;

    // ── 내부 분류용 raw 상태 ────────────────────────────────────────────────
    private enum RawStatus { OPEN, NO_PRICE, UNCONFIRMED }

    private record RawData(
            LocalDate date,
            RawStatus rawStatus,
            List<StockListing> listings,
            List<StockPrice> prices
    ) {
        static RawData open(LocalDate date, List<StockListing> listings, List<StockPrice> prices) {
            return new RawData(date, RawStatus.OPEN, listings, prices);
        }
        static RawData noPrice(LocalDate date) {
            return new RawData(date, RawStatus.NO_PRICE, List.of(), List.of());
        }
        static RawData unconfirmed(LocalDate date) {
            return new RawData(date, RawStatus.UNCONFIRMED, List.of(), List.of());
        }
    }

    // ── 공개 API ────────────────────────────────────────────────────────────

    /**
     * 날짜별로 가상 스레드를 생성해 병렬 조회하고, 완료 후 PENDING을 확정한다.
     *
     * <p>Semaphore로 동시 KRX API 요청 수를 {@code krx.fetch-concurrency}(기본 5)로 제한한다.
     */
    public List<DailyMarketData> fetch(MarketType market, LocalDate from, LocalDate to) {
        List<LocalDate> dates = from.datesUntil(to.plusDays(1)).toList();
        if (dates.isEmpty()) return List.of();

        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            List<CompletableFuture<RawData>> futures = dates.stream()
                    .map(date -> CompletableFuture.supplyAsync(
                            () -> fetchDateRaw(market, date), executor))
                    .toList();

            List<RawData> rawList = IntStream.range(0, dates.size())
                    .mapToObj(i -> {
                        try {
                            return futures.get(i).join();
                        } catch (CompletionException e) {
                            log.error("[{}] 예상치 못한 fetch 오류: {}", market, dates.get(i), e.getCause());
                            return RawData.unconfirmed(dates.get(i));
                        }
                    })
                    .toList();

            return resolve(rawList);
        }
    }

    // ── 날짜별 fetch (Semaphore 적용) ───────────────────────────────────────

    private RawData fetchDateRaw(MarketType market, LocalDate date) {
        try {
            semaphore.acquire();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return RawData.unconfirmed(date);
        }
        try {
            return doFetchDate(market, date);
        } finally {
            semaphore.release();
        }
    }

    private RawData doFetchDate(MarketType market, LocalDate date) {
        // 1. 종목 조회
        List<StockListing> listings;
        try {
            listings = fetchListings(market, date);
        } catch (FeignException.Unauthorized e) {
            log.error("[{}] 종목 조회 인증 오류: {}", market, date, e);
            return RawData.unconfirmed(date);
        } catch (FeignException e) {
            log.warn("[{}] 종목 조회 일시 오류: {}", market, date, e);
            return RawData.unconfirmed(date);
        }

        // 2. 주가 조회
        List<StockPrice> prices;
        try {
            prices = fetchPrices(market, date);
        } catch (FeignException.Unauthorized e) {
            log.error("[{}] 주가 조회 인증 오류: {}", market, date, e);
            return RawData.unconfirmed(date);
        } catch (FeignException e) {
            log.warn("[{}] 주가 조회 일시 오류: {}", market, date, e);
            return RawData.unconfirmed(date);
        } catch (PriceParseException | PriceResponseNullException e) {
            return RawData.unconfirmed(date);
        }

        // 3. 분류
        if (listings.isEmpty() && prices.isEmpty()) {
            log.debug("[{}] 종목·주가 모두 빈 응답(미확정): {}", market, date);
            return RawData.unconfirmed(date);
        } else if (!listings.isEmpty() && !prices.isEmpty()) {
            return RawData.open(date, listings, prices);
        } else {
            // 종목 있음, 주가 없음 → PENDING
            return RawData.noPrice(date);
        }
    }

    // ── PENDING 후처리 ──────────────────────────────────────────────────────

    private List<DailyMarketData> resolve(List<RawData> rawList) {
        // 병렬 처리로 완료 순서가 다를 수 있으므로 날짜 기준 정렬
        List<RawData> sorted = rawList.stream()
                .sorted(Comparator.comparing(RawData::date))
                .toList();

        LocalDate lastOpenDate = sorted.stream()
                .filter(r -> r.rawStatus() == RawStatus.OPEN)
                .map(RawData::date)
                .max(Comparator.naturalOrder())
                .orElse(null);

        List<DailyMarketData> result = new ArrayList<>();
        for (RawData r : sorted) {
            DailyMarketData data = switch (r.rawStatus()) {
                case OPEN -> DailyMarketData.open(r.date(), r.listings(), r.prices());
                case NO_PRICE -> {
                    boolean isClosed = lastOpenDate != null && r.date().isBefore(lastOpenDate);
                    yield isClosed
                            ? DailyMarketData.closed(r.date())
                            : DailyMarketData.unconfirmed(r.date());
                }
                case UNCONFIRMED -> DailyMarketData.unconfirmed(r.date());
            };
            result.add(data);
            // UNCONFIRMED 이후 날짜는 의미 없음
            if (data.status() == DayStatus.UNCONFIRMED) break;
        }
        return result;
    }

    // ── KRX API 호출 ────────────────────────────────────────────────────────

    private List<StockListing> fetchListings(MarketType market, LocalDate date) {
        KrxListedStockResponse response = switch (market) {
            case KOSPI -> krxStockClient.getKospiListedStocks(krxApiAuthKey, date);
            case KOSDAQ -> krxStockClient.getKosdaqListedStocks(krxApiAuthKey, date);
            case KONEX -> krxStockClient.getKonexListedStocks(krxApiAuthKey, date);
        };
        if (response == null || response.getItems() == null) return List.of();
        return response.getItems().stream()
                .filter(item -> item.getTicker() != null && !item.getTicker().isBlank())
                .map(KrxListedStockTranslator::translate)
                .toList();
    }

    private List<StockPrice> fetchPrices(MarketType market, LocalDate date) {
        KrxDailyPriceResponse response = switch (market) {
            case KOSPI -> krxStockClient.getDailyKospiStockInfo(krxApiAuthKey, date);
            case KOSDAQ -> krxStockClient.getDailyKosdaqStockInfo(krxApiAuthKey, date);
            case KONEX -> krxStockClient.getDailyKonexStockInfo(krxApiAuthKey, date);
        };
        if (response == null || response.getDataList() == null) {
            log.warn("[{}] 주가 응답 null: {}", market, date);
            throw new PriceResponseNullException();
        }
        if (response.getDataList().isEmpty()) {
            return List.of();
        }
        try {
            return response.getDataList().stream()
                    .map(KrxDailyStockPriceTranslator::translate)
                    .toList();
        } catch (RuntimeException e) {
            log.error("[{}] 주가 파싱 오류: {}", market, date, e);
            throw new PriceParseException(e);
        }
    }

    private static class PriceResponseNullException extends RuntimeException {}
    private static class PriceParseException extends RuntimeException {
        PriceParseException(Throwable cause) { super(cause); }
    }
}
