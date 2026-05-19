package com.dove.scheduler.service;

import com.dove.indicator.application.service.IndicatorBulkCalculateService;
import com.dove.indicator.application.service.TechnicalIndicatorQueryService;
import com.dove.indicator.domain.enums.IndicatorType;
import com.dove.market.domain.enums.MarketType;
import com.dove.stock.application.service.DailyStockPriceQueryService;
import com.dove.stock.domain.entity.DailyStockPrice;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.Future;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 누락된 기술적 지표를 탐지하고 재계산한다.
 *
 * <p>주가는 있으나 지표 누락인 (종목, 날짜) 쌍을 찾아 슬라이딩 윈도우로 재계산한다.
 *
 * <h3>탐지 방식</h3>
 * 특정 지표 하나의 존재 여부(센티넬) 대신, 해당 날짜에 저장된 지표 수가
 * {@code IndicatorType.values().length}와 일치하는 날짜만 "완성"으로 판단한다.
 * 새 지표를 추가하면 {@code expectedCount}가 자동으로 늘어나
 * 코드 변경 없이 기존 날짜들이 미완성으로 재감지된다.
 *
 * <h3>엣지케이스 처리</h3>
 * <ul>
 *   <li><b>신규 상장 종목</b>: 전체 주가 이력이 최대 필요 봉 수(현재 MACD 기준 130)보다
 *       적으면 계산을 건너뛴다. 충분한 이력이 쌓이면 다음 스캔에서 자동 처리된다.</li>
 *   <li><b>상장폐지 갭</b>: 갭 기간은 주가 행이 없으므로 priceDates에 포함되지 않아
 *       재시도 대상이 되지 않는다. 재상장 후 날짜는 전체 이력으로 정상 계산된다.</li>
 * </ul>
 *
 * <h3>성능 구조</h3>
 * <ul>
 *   <li>날짜 범위를 {@value #DATE_CHUNK_DAYS}일 단위 청크로 분할해 메모리 사용과
 *       진행 로그 단위를 조절한다.</li>
 *   <li>종목별 전체 주가 조회({@code findAllByMarketAndCode})는 스레드풀로 병렬화한다.</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class IndicatorCompletionService {

    private final DailyStockPriceQueryService dailyStockPriceQueryService;
    private final TechnicalIndicatorQueryService technicalIndicatorQueryService;
    private final IndicatorBulkCalculateService indicatorBulkCalculateService;
    private final ThreadPoolTaskExecutor taskExecutor;

    /** 종목 청크 크기 — 스레드풀 제출 단위 */
    private static final int STOCK_CHUNK_SIZE = 50;
    /** 날짜 청크 크기 — 메모리·로그 단위 (일) */
    private static final int DATE_CHUNK_DAYS = 90;

    public void scan(MarketType market, LocalDate from, LocalDate to) {
        long scanStart = System.currentTimeMillis();

        List<LocalDate[]> dateChunks = buildDateChunks(from, to, DATE_CHUNK_DAYS);
        int maxRequired = indicatorBulkCalculateService.maxRequiredDataSize();

        log.info("[{}] 지표 완성 스캔 시작: {}~{} / {}일-청크 {}개 / 최소봉={} ({})",
                market, from, to, DATE_CHUNK_DAYS, dateChunks.size(), maxRequired,
                IndicatorType.values().length + "개 지표");

        int totalStocks = 0;
        int totalDates  = 0;
        int totalSkipped = 0;

        for (int ci = 0; ci < dateChunks.size(); ci++) {
            LocalDate chunkFrom = dateChunks.get(ci)[0];
            LocalDate chunkTo   = dateChunks.get(ci)[1];

            long chunkStart = System.currentTimeMillis();
            log.debug("[{}] 청크 {}/{} 시작: {}~{}", market, ci + 1, dateChunks.size(), chunkFrom, chunkTo);

            ChunkStat stat = processChunk(market, chunkFrom, chunkTo, maxRequired);

            long elapsed = System.currentTimeMillis() - chunkStart;
            if (stat.hasWork()) {
                log.info("[{}] 청크 {}/{} 완료 ({}ms): 재계산 {}종목 {}날짜, 데이터부족 건너뜀 {}종목",
                        market, ci + 1, dateChunks.size(), elapsed,
                        stat.stocks(), stat.dates(), stat.skipped());
            } else {
                log.debug("[{}] 청크 {}/{} 완료 ({}ms): 누락 없음",
                        market, ci + 1, dateChunks.size(), elapsed);
            }

            totalStocks  += stat.stocks();
            totalDates   += stat.dates();
            totalSkipped += stat.skipped();
        }

        long totalElapsed = System.currentTimeMillis() - scanStart;
        log.info("[{}] 지표 완성 스캔 종료 (총 {}ms): 재계산 {}종목 {}날짜 / 데이터부족 건너뜀 {}종목",
                market, totalElapsed, totalStocks, totalDates, totalSkipped);
    }

    // -------------------------------------------------------------------------
    // 내부 구현
    // -------------------------------------------------------------------------

    private ChunkStat processChunk(MarketType market,
                                   LocalDate from, LocalDate to,
                                   int maxRequired) {
        // ── 1. 주가 날짜 조회 ──────────────────────────────────────────────────
        long t1 = System.currentTimeMillis();
        Map<String, Set<LocalDate>> priceDates =
                dailyStockPriceQueryService.findPriceDatesByStock(market, from, to);
        log.debug("[{}] {}~{} 주가 날짜 조회: {}종목 ({}ms)",
                market, from, to, priceDates.size(), System.currentTimeMillis() - t1);

        if (priceDates.isEmpty()) {
            return ChunkStat.EMPTY;
        }

        // ── 2. 완성 날짜 조회 ─────────────────────────────────────────────────
        long t2 = System.currentTimeMillis();
        long expectedCount = IndicatorType.values().length;
        Map<String, Set<LocalDate>> completedDates =
                technicalIndicatorQueryService.findCompletedDatesPerStock(market, from, to, expectedCount);
        log.debug("[{}] {}~{} 완성 날짜 조회: {}종목 ({}ms)",
                market, from, to, completedDates.size(), System.currentTimeMillis() - t2);

        // ── 3. 누락 종목 추출 + 병렬 계산 ────────────────────────────────────
        AtomicInteger stockCount   = new AtomicInteger();
        AtomicInteger dateCount    = new AtomicInteger();
        AtomicInteger skippedCount = new AtomicInteger();
        int totalStocks = priceDates.size();

        List<Map.Entry<String, Set<LocalDate>>> entries = new ArrayList<>(priceDates.entrySet());
        List<List<Map.Entry<String, Set<LocalDate>>>> stockChunks = partition(entries, STOCK_CHUNK_SIZE);

        List<Future<?>> futures = new ArrayList<>();
        for (List<Map.Entry<String, Set<LocalDate>>> stockChunk : stockChunks) {
            futures.add(taskExecutor.submit(() -> {
                for (Map.Entry<String, Set<LocalDate>> entry : stockChunk) {
                    String code = entry.getKey();
                    TreeSet<LocalDate> stockPriceDates = new TreeSet<>(entry.getValue());
                    Set<LocalDate> stockCompletedDates = completedDates.getOrDefault(code, Set.of());

                    Set<LocalDate> missingDates = new HashSet<>(stockPriceDates);
                    missingDates.removeAll(stockCompletedDates);
                    if (missingDates.isEmpty()) continue;

                    // 전체 주가 이력 조회 (슬라이딩 윈도우 계산용)
                    List<DailyStockPrice> allPrices =
                            dailyStockPriceQueryService.findAllByMarketAndCode(market, code);

                    // 신규 상장 등 데이터 부족 종목 건너뜀
                    if (allPrices.size() < maxRequired) {
                        log.debug("[{}] {} 주가 부족 ({} < {}봉) — 지표 계산 건너뜀 (데이터 부족)",
                                market, code, allPrices.size(), maxRequired);
                        skippedCount.incrementAndGet();
                        continue;
                    }

                    LocalDate cumulativeRecalcFrom = missingDates.stream()
                            .min(LocalDate::compareTo)
                            .orElseThrow();

                    indicatorBulkCalculateService.calculateAndSave(
                            market, code, allPrices, missingDates, cumulativeRecalcFrom);

                    int done = stockCount.incrementAndGet();
                    dateCount.addAndGet(missingDates.size());

                    if (done % 100 == 0) {
                        log.info("[{}] {}~{} 지표 계산 진행: {}/{} 종목",
                                market, from, to, done, totalStocks);
                    }
                }
            }));
        }
        waitAll(futures);

        return new ChunkStat(stockCount.get(), dateCount.get(), skippedCount.get());
    }

    // -------------------------------------------------------------------------
    // 유틸
    // -------------------------------------------------------------------------

    private static List<LocalDate[]> buildDateChunks(LocalDate from, LocalDate to, int chunkDays) {
        List<LocalDate[]> chunks = new ArrayList<>();
        LocalDate start = from;
        while (!start.isAfter(to)) {
            LocalDate end = start.plusDays(chunkDays - 1);
            if (end.isAfter(to)) end = to;
            chunks.add(new LocalDate[]{start, end});
            start = end.plusDays(1);
        }
        return chunks;
    }

    private static <T> List<List<T>> partition(List<T> list, int size) {
        List<List<T>> result = new ArrayList<>();
        for (int i = 0; i < list.size(); i += size) {
            result.add(list.subList(i, Math.min(i + size, list.size())));
        }
        return result;
    }

    private static void waitAll(List<Future<?>> futures) {
        for (Future<?> future : futures) {
            try {
                future.get();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("지표 완성 계산 중 인터럽트 발생", e);
            } catch (ExecutionException e) {
                throw new RuntimeException("지표 완성 계산 중 오류 발생", e.getCause());
            }
        }
    }

    // -------------------------------------------------------------------------
    // 내부 타입
    // -------------------------------------------------------------------------

    /** 청크 단위 처리 통계 */
    private record ChunkStat(int stocks, int dates, int skipped) {
        static final ChunkStat EMPTY = new ChunkStat(0, 0, 0);

        boolean hasWork() {
            return stocks > 0 || skipped > 0;
        }
    }
}
