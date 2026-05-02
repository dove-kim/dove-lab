package com.dove.technicalindicator.application.service;

import com.dove.market.application.service.MarketTradingDateQueryService;
import com.dove.market.domain.enums.MarketType;
import com.dove.stockprice.application.service.DailyStockPriceQueryService;
import com.dove.stockprice.domain.entity.DailyStockPrice;
import com.dove.technicalindicator.domain.calculator.TechnicalIndicatorCalculator;
import com.dove.technicalindicator.domain.entity.IndicatorCursor;
import com.dove.technicalindicator.domain.entity.TechnicalIndicator;
import com.dove.technicalindicator.domain.enums.DateState;
import com.dove.technicalindicator.domain.enums.IndicatorType;
import com.dove.technicalindicator.domain.repository.TechnicalIndicatorRepository;
import com.dove.technicalindicator.domain.service.IndicatorDateClassifier;
import com.dove.technicalindicator.domain.service.LookbackCollector;
import com.dove.technicalindicator.infrastructure.config.IndicatorCursorProperties;
import com.dove.technicalindicator.infrastructure.repository.TechnicalIndicatorQueryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class IndicatorCursorAdvanceService {

    private final List<TechnicalIndicatorCalculator> calculators;
    private final IndicatorCursorCommandService cursorCommandService;
    private final DailyStockPriceQueryService dailyStockPriceQueryService;
    private final MarketTradingDateQueryService marketTradingDateQueryService;
    private final IndicatorDateClassifier dateClassifier;
    private final LookbackCollector lookbackCollector;
    private final TechnicalIndicatorRepository technicalIndicatorRepository;
    private final TechnicalIndicatorQueryRepository indicatorQueryRepository;
    private final IndicatorCursorProperties cursorProperties;

    /**
     * 지정된 calculator 한 칸 전진.
     * insertedDate 처리:
     *   - insertedDate < cursorNext: 과거 데이터 변경 → rewind 후 재진행
     *   - insertedDate > cursorNext: 커서 미도달 → 무시(empty)
     *   - insertedDate == cursorNext or null: 정상 진행
     * 전진 성공 시 다음 계산 대상일을 Optional로 반환 (체이닝 메시지 발행 판단용).
     */
    @Transactional
    public Optional<LocalDate> advance(MarketType market, String code,
                                       IndicatorType indicatorType, LocalDate insertedDate) {
        IndicatorCursor cursor = cursorCommandService.getOrCreate(
                market, code, indicatorType, cursorProperties.getInitialDate());
        LocalDate nextDay = skipWeekends(cursor.getLastCalculatedDate().plusDays(1));

        if (insertedDate != null) {
            if (insertedDate.isBefore(nextDay)) {
                cursorCommandService.rewindIfBefore(market, code, indicatorType, insertedDate);
                cursor = cursorCommandService.getOrCreate(
                        market, code, indicatorType, cursorProperties.getInitialDate());
                nextDay = skipWeekends(cursor.getLastCalculatedDate().plusDays(1));
            } else if (insertedDate.isAfter(nextDay)) {
                return Optional.empty();
            }
        }

        Optional<LocalDate> latestOpt = dailyStockPriceQueryService.findLatestTradeDateByMarket(market);
        if (latestOpt.isEmpty()) return Optional.empty();
        LocalDate latestPriceDate = latestOpt.get();

        TechnicalIndicatorCalculator calculator = findCalculator(indicatorType);

        if (nextDay.isAfter(latestPriceDate)) return Optional.empty();

        if (!marketTradingDateQueryService.existsOpenDay(market, nextDay)) {
            cursor.advance(nextDay);
            return Optional.of(skipWeekends(nextDay.plusDays(1)));
        }

        DateState state = dateClassifier.classify(market, code, nextDay);
        return switch (state) {
            case ACTIVE -> {
                List<DailyStockPrice> pool = lookbackCollector.collect(
                        market, code, nextDay, calculator.requiredDataSize());
                if (pool.size() == calculator.requiredDataSize()) {
                    double seed = fetchObvSeed(market, code, pool, nextDay, calculator);
                    Map<IndicatorType, Double> results = calculator.calculateWithSeed(pool, seed);
                    saveAll(market, code, nextDay, results);
                }
                cursor.advance(nextDay);
                yield Optional.of(skipWeekends(nextDay.plusDays(1)));
            }
            case SUSPENDED, DELISTED -> {
                cursor.advance(nextDay);
                yield Optional.of(skipWeekends(nextDay.plusDays(1)));
            }
            case LISTING_NOT_SYNCED -> Optional.empty();
        };
    }

    private TechnicalIndicatorCalculator findCalculator(IndicatorType indicatorType) {
        return calculators.stream()
                .filter(c -> c.cursorType() == indicatorType)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown indicator type: " + indicatorType));
    }

    private static LocalDate skipWeekends(LocalDate date) {
        while (date.getDayOfWeek() == DayOfWeek.SATURDAY || date.getDayOfWeek() == DayOfWeek.SUNDAY) {
            date = date.plusDays(1);
        }
        return date;
    }

    private double fetchObvSeed(MarketType market, String code,
                                List<DailyStockPrice> pool, LocalDate nextDay,
                                TechnicalIndicatorCalculator calculator) {
        if (calculator.cursorType() != IndicatorType.OBV) return 0.0;
        LocalDate poolOldestDate = pool.get(0).getId().getTradeDate();
        return indicatorQueryRepository
                .findLatestObvValue(market, code, poolOldestDate, nextDay)
                .orElse(0.0);
    }

    private void saveAll(MarketType market, String code, LocalDate tradeDate,
                         Map<IndicatorType, Double> results) {
        List<TechnicalIndicator> entities = new ArrayList<>();
        results.forEach((type, value) ->
                entities.add(new TechnicalIndicator(market, code, tradeDate, type, value)));
        technicalIndicatorRepository.saveAll(entities);
    }
}
