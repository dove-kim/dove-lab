package com.dove.scheduler.service;

import com.dove.krx.DailyMarketData;
import com.dove.krx.DayStatus;
import com.dove.krx.infrastructure.adapter.KrxTradingDayFetcher;
import com.dove.market.application.service.MarketTradingDateService;
import com.dove.market.domain.enums.MarketType;
import com.dove.scheduler.dto.ScanResult;
import com.dove.stock.application.service.DailyStockPriceQueryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 시장 데이터 수집·저장을 조율하는 공유 서비스.
 *
 * <p>DailyMarketJob(범위 직접 지정)과 CompletionScanJob(누락 탐지 후 제한 수집)이
 * 이 서비스를 공유한다. 조회는 TradingDayFetcher, 저장은 MarketDayWriter에 위임한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MarketDataCompletionService {

    private final MarketTradingDateService marketTradingDateService;
    private final DailyStockPriceQueryService dailyStockPriceQueryService;
    private final KrxTradingDayFetcher krxTradingDayFetcher;
    private final MarketDayWriter marketDayWriter;

    /**
     * DailyMarketJob 전용 — 지정 범위를 그대로 수집·저장한다.
     *
     * @return OPEN으로 확정된 날짜 목록
     */
    public List<LocalDate> syncRange(MarketType market, LocalDate from, LocalDate upTo) {
        log.info("[{}] 범위 수집 시작: {}~{}", market, from, upTo);
        List<DailyMarketData> days = krxTradingDayFetcher.fetch(market, from, upTo);
        List<LocalDate> openDates = persistAll(market, days);
        log.info("[{}] 범위 수집 완료: OPEN {}건", market, openDates.size());
        return openDates;
    }

    /**
     * CompletionScanJob 전용 — 누락 날짜를 탐지해 최대 {@code maxDates}건 수집·저장한다.
     *
     * <p>가장 오래된 날짜부터 처리하며, 잔여 누락분은 다음 실행으로 자동 이월된다.
     */
    public ScanResult scan(MarketType market, LocalDate from, LocalDate upTo, int maxDates) {
        Set<LocalDate> existingPriceDates =
                dailyStockPriceQueryService.findExistingTradeDatesInRange(market, from, upTo);

        // MTD에서 CLOSED 확정된 날짜는 재수집 불필요
        Set<LocalDate> confirmedClosed = marketTradingDateService.findStatusesInRange(market, from, upTo)
                .entrySet().stream()
                .filter(e -> !e.getValue())
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());

        List<LocalDate> allMissing = from.datesUntil(upTo.plusDays(1))
                .filter(d -> d.getDayOfWeek().getValue() <= 5) // 평일만
                .filter(d -> !existingPriceDates.contains(d))
                .filter(d -> !confirmedClosed.contains(d))
                .toList();

        if (allMissing.isEmpty()) {
            log.info("[{}] 시장 데이터 누락 없음 ({}~{})", market, from, upTo);
            return ScanResult.skipped(market);
        }

        // API 호출 한도 적용 — 가장 오래된 날짜부터 처리
        List<LocalDate> missingDates = allMissing.size() > maxDates
                ? allMissing.subList(0, maxDates)
                : allMissing;

        if (allMissing.size() > maxDates) {
            log.info("[{}] 누락 {}건 중 {}건만 처리 (잔여 {}건은 다음 실행으로 이월)",
                    market, allMissing.size(), maxDates, allMissing.size() - maxDates);
        }

        LocalDate rangeFrom = missingDates.get(0);
        LocalDate rangeTo = missingDates.get(missingDates.size() - 1);
        log.info("[{}] 시장 데이터 누락 {}건 보정 시작 ({}~{})", market, missingDates.size(), rangeFrom, rangeTo);

        List<DailyMarketData> days = krxTradingDayFetcher.fetch(market, rangeFrom, rangeTo);
        List<LocalDate> openDates = persistAll(market, days);

        log.info("[{}] 시장 데이터 보정 완료: OPEN {}건", market, openDates.size());
        return new ScanResult(market, missingDates.size(), openDates.size(), List.of());
    }

    // ── 공통 저장 루프 ──────────────────────────────────────────────────────

    private List<LocalDate> persistAll(MarketType market, List<DailyMarketData> days) {
        List<LocalDate> openDates = new ArrayList<>();
        for (DailyMarketData day : days) {
            try {
                marketDayWriter.write(market, day);
                if (day.status() == DayStatus.OPEN) {
                    openDates.add(day.date());
                }
            } catch (Exception e) {
                log.warn("[{}] {} 저장 실패, 다음 실행으로 이월: {}", market, day.date(), e.getMessage());
            }
        }
        return openDates;
    }
}
