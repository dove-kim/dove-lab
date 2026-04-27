package com.dove.technicalindicator.application.service;

import com.dove.stockprice.application.service.DailyStockPriceQueryService;
import com.dove.stockprice.domain.entity.DailyStockPrice;
import com.dove.market.domain.enums.MarketType;
import com.dove.technicalindicator.domain.calculator.TechnicalIndicatorCalculator;
import com.dove.technicalindicator.domain.entity.TechnicalIndicator;
import com.dove.technicalindicator.domain.enums.IndicatorType;
import com.dove.technicalindicator.domain.repository.TechnicalIndicatorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * 기술적 지표 계산 서비스.
 * 등록된 모든 계산기를 사용하여 종목별 지표를 계산하고, 데이터 부족 시 건너뛴다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TechnicalIndicatorCalculationService {

    private final DailyStockPriceQueryService dailyStockPriceQueryService;
    private final TechnicalIndicatorRepository technicalIndicatorRepository;
    private final List<TechnicalIndicatorCalculator> calculators;

    @Transactional
    public void calculateForStock(MarketType marketType, String stockCode, LocalDate tradeDate) {
        for (TechnicalIndicatorCalculator calculator : calculators) {
            calculateSingle(calculator, marketType, stockCode, tradeDate);
        }
    }

    /**
     * 해당 날짜의 지표를 계산하고, 이후 날짜에 존재하는 데이터도 모두 재계산한다.
     * 비순차 데이터 도착 시(예: 10/5 → 10/2 순서) 과거 데이터 변경이 미래 지표에
     * 영향을 미치므로, 해당 종목의 모든 미래 날짜를 무조건 재계산한다.
     *
     * 동시성 보장: 이 메서드를 호출하는 Kafka 리스너는 concurrency=1로 설정하여
     * 동일 시장 내 이벤트가 순차 처리되도록 보장해야 한다.
     */
    @Transactional
    public void calculateWithRecalculation(MarketType marketType, String stockCode, LocalDate tradeDate) {
        calculateForStock(marketType, stockCode, tradeDate);

        List<LocalDate> futureDates = dailyStockPriceQueryService.findTradeDatesAfter(
                marketType, stockCode, tradeDate);

        for (LocalDate futureDate : futureDates) {
            log.debug("후속 날짜 재계산 - 종목: {}, 날짜: {}", stockCode, futureDate);
            calculateForStock(marketType, stockCode, futureDate);
        }
    }

    private void calculateSingle(TechnicalIndicatorCalculator calculator,
                                 MarketType marketType, String stockCode, LocalDate tradeDate) {
        try {
            int requiredSize = calculator.requiredDataSize();
            List<DailyStockPrice> recentData = dailyStockPriceQueryService.findRecentDailyStockPrice(
                    marketType, stockCode, tradeDate, requiredSize);

            if (recentData.size() < requiredSize) {
                log.debug("데이터 부족으로 {} 지표 계산 건너뜀 - 종목: {}, 필요: {}, 보유: {}",
                        calculator.getName(), stockCode, requiredSize, recentData.size());
                return;
            }

            List<DailyStockPrice> ascData = new ArrayList<>(recentData);
            ascData.sort(Comparator.comparing(sd -> sd.getId().getTradeDate()));

            Map<IndicatorType, Double> results = calculator.calculate(ascData);

            for (Map.Entry<IndicatorType, Double> entry : results.entrySet()) {
                technicalIndicatorRepository.save(new TechnicalIndicator(
                        marketType, stockCode, tradeDate,
                        entry.getKey(), entry.getValue()));
            }
        } catch (Exception e) {
            log.error("{} 지표 계산 실패 - 종목: {}, 날짜: {}, 오류: {}",
                    calculator.getName(), stockCode, tradeDate, e.getMessage(), e);
        }
    }
}
