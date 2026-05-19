package com.dove.scheduler.service;

import com.dove.indicator.application.service.TechnicalIndicatorCommandService;
import com.dove.indicator.application.service.TechnicalIndicatorQueryService;
import com.dove.indicator.domain.calculator.TechnicalIndicatorCalculator;
import com.dove.indicator.domain.entity.TechnicalIndicator;
import com.dove.indicator.domain.enums.IndicatorType;
import com.dove.market.domain.enums.MarketType;
import com.dove.stock.application.service.DailyStockPriceQueryService;
import com.dove.stock.domain.entity.DailyStockPrice;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 * 일별 기술적 지표를 계산하고 저장한다.
 *
 * <p>{@code calculateAndSave}: 특정 날짜의 전체 종목을 청크 단위로 병렬 처리.
 * {@code calculateRange}: 범위 내 미계산 (종목, 날짜) 쌍만 필터링해 순차 처리.
 */
@Slf4j
@Service
public class IndicatorDailyCalculateService {

    private final DailyStockPriceQueryService dailyStockPriceQueryService;
    private final TechnicalIndicatorQueryService technicalIndicatorQueryService;
    private final TechnicalIndicatorCommandService technicalIndicatorCommandService;
    private final List<TechnicalIndicatorCalculator> calculators;
    private final ThreadPoolTaskExecutor taskExecutor;

    private final int maxLookback;
    private final int chunkSize;
    private final Set<IndicatorType> cumulativeTypes;

    public IndicatorDailyCalculateService(
            DailyStockPriceQueryService dailyStockPriceQueryService,
            TechnicalIndicatorQueryService technicalIndicatorQueryService,
            TechnicalIndicatorCommandService technicalIndicatorCommandService,
            List<TechnicalIndicatorCalculator> calculators,
            ThreadPoolTaskExecutor taskExecutor) {
        this.dailyStockPriceQueryService = dailyStockPriceQueryService;
        this.technicalIndicatorQueryService = technicalIndicatorQueryService;
        this.technicalIndicatorCommandService = technicalIndicatorCommandService;
        this.calculators = calculators;
        this.taskExecutor = taskExecutor;

        this.maxLookback = calculators.stream()
                .mapToInt(TechnicalIndicatorCalculator::requiredDataSize)
                .max()
                .orElse(1);
        this.chunkSize = Math.max(50, maxLookback * 3);

        Set<IndicatorType> types = new HashSet<>();
        for (TechnicalIndicatorCalculator calc : calculators) {
            if (calc.isCumulative()) {
                types.add(calc.indicatorType());
            }
        }
        this.cumulativeTypes = types;
    }

    public void calculateAndSave(MarketType market, LocalDate date) {
        List<String> stockCodes = dailyStockPriceQueryService
                .findStockCodesByMarketTypeAndTradeDate(market, date);
        if (stockCodes.isEmpty()) {
            log.debug("[{}] {} 종목 없음 — 지표 계산 건너뜀", market, date);
            return;
        }

        Map<String, Map<IndicatorType, Double>> seeds = loadSeeds(market, date);

        List<List<String>> chunks = partition(stockCodes, chunkSize);
        List<Future<?>> futures = new ArrayList<>();
        for (List<String> chunk : chunks) {
            futures.add(taskExecutor.submit(
                    () -> chunk.forEach(code -> processStock(market, code, date, seeds))));
        }
        waitAll(futures);
    }

    public void calculateRange(MarketType market, LocalDate from, LocalDate to) {
        Set<LocalDate> tradeDates = dailyStockPriceQueryService
                .findExistingTradeDatesInRange(market, from, to);
        if (tradeDates.isEmpty()) {
            log.debug("[{}] {}~{} 거래일 없음 — 지표 범위 계산 건너뜀", market, from, to);
            return;
        }

        IndicatorType sentinel = calculators.get(0).indicatorType();
        Map<String, Set<LocalDate>> calculatedDates = technicalIndicatorQueryService
                .findCalculatedDatesPerStock(market, sentinel, from, to);

        TreeSet<LocalDate> sortedDates = new TreeSet<>(tradeDates);

        for (LocalDate date : sortedDates) {
            List<String> stockCodes = dailyStockPriceQueryService
                    .findStockCodesByMarketTypeAndTradeDate(market, date);

            List<String> targets = new ArrayList<>();
            for (String code : stockCodes) {
                Set<LocalDate> done = calculatedDates.getOrDefault(code, Set.of());
                if (!done.contains(date)) {
                    targets.add(code);
                }
            }

            if (targets.isEmpty()) continue;

            Map<String, Map<IndicatorType, Double>> seeds = loadSeeds(market, date);
            List<List<String>> chunks = partition(targets, chunkSize);
            List<Future<?>> futures = new ArrayList<>();
            for (List<String> chunk : chunks) {
                futures.add(taskExecutor.submit(
                        () -> chunk.forEach(code -> processStock(market, code, date, seeds))));
            }
            waitAll(futures);
        }
    }

    // ── private helpers ──────────────────────────────────────────────────────

    private Map<String, Map<IndicatorType, Double>> loadSeeds(MarketType market, LocalDate date) {
        if (cumulativeTypes.isEmpty()) {
            return Map.of();
        }
        Optional<LocalDate> prevDate = dailyStockPriceQueryService
                .findNthRecentTradeDate(List.of(market), date, false, 0);
        if (prevDate.isEmpty()) {
            return Map.of();
        }
        return technicalIndicatorQueryService
                .findSeedsByMarketAndDate(market, prevDate.get(), cumulativeTypes);
    }

    private void processStock(MarketType market, String code, LocalDate date,
                              Map<String, Map<IndicatorType, Double>> seeds) {
        List<DailyStockPrice> prices = dailyStockPriceQueryService
                .findRecentDailyStockPrice(market, code, date, maxLookback);
        if (prices.isEmpty()) return;

        List<TechnicalIndicator> toSave = new ArrayList<>();
        Map<IndicatorType, Double> stockSeeds = seeds.getOrDefault(code, Map.of());

        for (TechnicalIndicatorCalculator calc : calculators) {
            if (prices.size() < calc.requiredDataSize()) continue;

            Map<IndicatorType, Double> result;
            if (calc.isCumulative()) {
                double seed = stockSeeds.getOrDefault(calc.indicatorType(), 0.0);
                if (seed != 0.0) {
                    result = calc.calculateWithSeed(prices, seed);
                } else {
                    result = calc.calculate(prices);
                }
            } else {
                result = calc.calculate(prices);
            }

            for (Map.Entry<IndicatorType, Double> entry : result.entrySet()) {
                toSave.add(new TechnicalIndicator(
                        market, code, date, entry.getKey(), entry.getValue()));
            }
        }

        if (!toSave.isEmpty()) {
            technicalIndicatorCommandService.saveAll(toSave);
        }
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
                throw new RuntimeException("지표 계산 중 인터럽트 발생", e);
            } catch (ExecutionException e) {
                throw new RuntimeException("지표 계산 중 오류 발생", e.getCause());
            }
        }
    }
}
