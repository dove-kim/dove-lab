package com.dove.indicator.application.service;

import com.dove.market.domain.enums.MarketType;
import com.dove.indicator.domain.enums.IndicatorType;
import com.dove.indicator.infrastructure.repository.TechnicalIndicatorQueryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TechnicalIndicatorQueryService {

    private final TechnicalIndicatorQueryRepository technicalIndicatorQueryRepository;

    public Map<String, Map<IndicatorType, Double>> findAllByMarketsAndDate(List<MarketType> markets, LocalDate date) {
        return technicalIndicatorQueryRepository.findAllByMarketsAndDate(markets, date);
    }

    public Map<LocalDate, Map<IndicatorType, Double>> findRecentByStock(
            MarketType market, String code, List<IndicatorType> types, int limit) {
        return technicalIndicatorQueryRepository.findRecentByStock(market, code, types, limit);
    }

    /** 시장·지표타입·기간 내 종목별 계산 완료 거래일 집합 반환. 스캐너의 누락 검사에 사용. */
    public Map<String, Set<LocalDate>> findCalculatedDatesPerStock(
            MarketType market, IndicatorType indicatorType, LocalDate from, LocalDate to) {
        return technicalIndicatorQueryRepository.findCalculatedDatesPerStock(market, indicatorType, from, to);
    }

    /**
     * 모든 지표가 완성된 (종목, 거래일) 집합 반환.
     * expectedCount와 저장된 지표 수가 일치해야 완성으로 판단하므로
     * 새 지표 추가 시 자동으로 기존 날짜들이 미완성으로 재감지된다.
     */
    public Map<String, Set<LocalDate>> findCompletedDatesPerStock(
            MarketType market, LocalDate from, LocalDate to, long expectedCount) {
        return technicalIndicatorQueryRepository.findCompletedDatesPerStock(market, from, to, expectedCount);
    }

    /** 누적 지표 시드 조회 — targetDate 직전 가장 최근 계산값 반환. 값 없으면 0.0. */
    public double findLatestValueBefore(MarketType market, String code,
                                        IndicatorType indicatorType, LocalDate beforeDateExclusive) {
        return technicalIndicatorQueryRepository
                .findLatestValueBefore(market, code, indicatorType, beforeDateExclusive)
                .orElse(0.0);
    }

    /** 특정 거래일·타입 목록의 전체 종목 시드 벌크 조회. key = stockCode, value = (IndicatorType → 값).
     *  IndicatorDailyCalculateService 에서 병렬 작업 전 전체 시드를 한 번에 선조회하는 데 사용된다. */
    public Map<String, Map<IndicatorType, Double>> findSeedsByMarketAndDate(
            MarketType market, LocalDate date, Set<IndicatorType> types) {
        return technicalIndicatorQueryRepository.findByMarketAndDateAndTypes(market, date, types);
    }
}
