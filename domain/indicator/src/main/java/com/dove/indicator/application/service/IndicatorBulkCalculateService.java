package com.dove.indicator.application.service;

import com.dove.market.domain.enums.MarketType;
import com.dove.stock.domain.entity.DailyStockPrice;
import com.dove.indicator.domain.calculator.TechnicalIndicatorCalculator;
import com.dove.indicator.domain.entity.TechnicalIndicator;
import com.dove.indicator.domain.enums.IndicatorType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 종목별 전체 주가를 in-memory 슬라이딩 윈도우로 순회해 기술적 지표를 계산하고 벌크 저장한다.
 *
 * <p>누락 날짜 보정(12:00 CompletionScanJob)에 사용된다.
 * 누적 지표(EMA·OBV 등)는 시작부터 끝까지 순차 계산해야 정확한 값을 얻는다.
 * 비누적 지표는 {@code targetDates}만 저장하고,
 * 누적 지표는 {@code cumulativeRecalcFrom} 이후 모든 날짜를 덮어써 시드 오염 전파를 차단한다.
 */
@Service
@RequiredArgsConstructor
public class IndicatorBulkCalculateService {

    private final TechnicalIndicatorCommandService technicalIndicatorCommandService;
    private final List<TechnicalIndicatorCalculator> calculators;

    /** 등록된 계산기 중 가장 큰 최소 데이터 요구량. 신규 상장 종목 건너뜀 판단에 사용. */
    public int maxRequiredDataSize() {
        return calculators.stream()
                .mapToInt(TechnicalIndicatorCalculator::requiredDataSize)
                .max()
                .orElse(0);
    }

    /**
     * 주가 이력을 슬라이딩 윈도우로 순회해 지표를 계산하고 저장한다.
     *
     * <p>비누적 지표는 {@code targetDates}만 저장한다.
     * 누적 지표(EMA·OBV 등)는 {@code cumulativeRecalcFrom} 이후 전 거래일을 덮어써
     * 시드 오염 전파를 차단한다.
     */
    public void calculateAndSave(MarketType market, String stockCode,
                                  List<DailyStockPrice> allPricesAsc,
                                  Set<LocalDate> targetDates,
                                  LocalDate cumulativeRecalcFrom) {
        if (allPricesAsc.isEmpty() || targetDates.isEmpty()) return;

        List<TechnicalIndicator> toSave = new ArrayList<>();

        for (TechnicalIndicatorCalculator calc : calculators) {
            int required = calc.requiredDataSize();
            double seed = 0.0;
            boolean firstFullPool = true;

            for (int i = 0; i < allPricesAsc.size(); i++) {
                LocalDate date = allPricesAsc.get(i).getId().getTradeDate();
                int poolStart = Math.max(0, i - required + 1);
                List<DailyStockPrice> pool = allPricesAsc.subList(poolStart, i + 1);

                if (pool.size() < required) {
                    // 풀이 부족한 날짜는 계산 불가 — 누적 지표도 아직 시드가 없으므로 스킵
                    continue;
                }

                Map<IndicatorType, Double> result;
                if (calc.isCumulative()) {
                    if (firstFullPool) {
                        result = calc.calculate(pool);
                        firstFullPool = false;
                    } else {
                        result = calc.calculateWithSeed(pool, seed);
                    }
                    seed = result.getOrDefault(calc.indicatorType(), seed);
                    // 누적 지표: cumulativeRecalcFrom 이후는 전부 저장 (시드 오염 전파 차단)
                    if (date.isBefore(cumulativeRecalcFrom)) continue;
                } else {
                    if (!targetDates.contains(date)) continue; // 비누적은 저장 대상만 계산
                    result = calc.calculate(pool);
                }

                for (Map.Entry<IndicatorType, Double> entry : result.entrySet()) {
                    toSave.add(new TechnicalIndicator(
                            market, stockCode, date, entry.getKey(), entry.getValue()));
                }
            }
        }

        technicalIndicatorCommandService.saveAll(toSave);
    }
}
