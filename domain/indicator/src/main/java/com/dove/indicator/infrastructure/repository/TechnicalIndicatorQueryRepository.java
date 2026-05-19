package com.dove.indicator.infrastructure.repository;

import com.dove.market.domain.enums.MarketType;
import com.dove.indicator.domain.enums.IndicatorType;

import static com.dove.indicator.domain.entity.QTechnicalIndicator.technicalIndicator;
import com.querydsl.core.Tuple;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

@Repository
@RequiredArgsConstructor
public class TechnicalIndicatorQueryRepository {

    private final JPAQueryFactory queryFactory;

    public Optional<Double> findLatestObvValue(MarketType market, String code,
                                               LocalDate fromDateInclusive,
                                               LocalDate beforeDateExclusive) {
        Double value = queryFactory
                .select(technicalIndicator.indicatorValue)
                .from(technicalIndicator)
                .where(
                        technicalIndicator.id.marketType.eq(market),
                        technicalIndicator.id.stockCode.eq(code),
                        technicalIndicator.id.indicatorName.eq(IndicatorType.OBV),
                        technicalIndicator.id.tradeDate.goe(fromDateInclusive),
                        technicalIndicator.id.tradeDate.lt(beforeDateExclusive)
                )
                .orderBy(technicalIndicator.id.tradeDate.desc())
                .limit(1)
                .fetchOne();

        return Optional.ofNullable(value);
    }

    /** 특정 거래일의 시장별 전체 기술적 지표 벌크 조회.
     *  반환 key = stockCode, value = (IndicatorType → 값) 맵. */
    public Map<String, Map<IndicatorType, Double>> findAllByMarketsAndDate(List<MarketType> markets, LocalDate date) {
        List<Tuple> rows = queryFactory
                .select(
                        technicalIndicator.id.stockCode,
                        technicalIndicator.id.indicatorName,
                        technicalIndicator.indicatorValue
                )
                .from(technicalIndicator)
                .where(
                        technicalIndicator.id.marketType.in(markets),
                        technicalIndicator.id.tradeDate.eq(date)
                )
                .fetch();

        Map<String, Map<IndicatorType, Double>> result = new HashMap<>();
        for (Tuple row : rows) {
            String code = row.get(technicalIndicator.id.stockCode);
            IndicatorType type = row.get(technicalIndicator.id.indicatorName);
            Double value = row.get(technicalIndicator.indicatorValue);
            if (code != null && type != null && value != null) {
                result.computeIfAbsent(code, k -> new EnumMap<>(IndicatorType.class)).put(type, value);
            }
        }
        return result;
    }

    /** 시장·지표타입·기간 내 종목별 계산 완료 거래일 집합 반환.
     *  key = stockCode, value = 오름차순 거래일 집합. 스캐너의 누락 검사에 사용. */
    public Map<String, Set<LocalDate>> findCalculatedDatesPerStock(
            MarketType market, IndicatorType indicatorType, LocalDate from, LocalDate to) {

        List<Tuple> rows = queryFactory
                .select(technicalIndicator.id.stockCode, technicalIndicator.id.tradeDate)
                .from(technicalIndicator)
                .where(
                        technicalIndicator.id.marketType.eq(market),
                        technicalIndicator.id.indicatorName.eq(indicatorType),
                        technicalIndicator.id.tradeDate.between(from, to)
                )
                .fetch();

        Map<String, Set<LocalDate>> result = new HashMap<>();
        for (Tuple row : rows) {
            String code = row.get(technicalIndicator.id.stockCode);
            LocalDate date = row.get(technicalIndicator.id.tradeDate);
            if (code != null && date != null) {
                result.computeIfAbsent(code, k -> new TreeSet<>()).add(date);
            }
        }
        return result;
    }

    /**
     * 지표가 모두 계산 완료된 (종목, 거래일) 집합 반환.
     *
     * <p>특정 지표 하나의 존재 여부(센티넬 방식) 대신, 해당 날짜에 저장된 지표 수가
     * expectedCount와 일치하는 날짜만 "완성"으로 판단한다.
     * 새 지표가 추가되면 expectedCount가 자동으로 늘어나므로 코드 변경 없이
     * 기존 날짜들이 미완성으로 재감지된다.
     *
     * <p>PK가 (MARKET_TYPE, STOCK_CODE, TRADE_DATE, INDICATOR_NAME)이므로
     * COUNT(*) = COUNT(DISTINCT INDICATOR_NAME)이 보장된다.
     *
     * @param expectedCount 완성 기준 지표 수 (보통 {@code IndicatorType.values().length})
     */
    public Map<String, Set<LocalDate>> findCompletedDatesPerStock(
            MarketType market, LocalDate from, LocalDate to, long expectedCount) {

        List<Tuple> rows = queryFactory
                .select(technicalIndicator.id.stockCode, technicalIndicator.id.tradeDate)
                .from(technicalIndicator)
                .where(
                        technicalIndicator.id.marketType.eq(market),
                        technicalIndicator.id.tradeDate.between(from, to)
                )
                .groupBy(technicalIndicator.id.stockCode, technicalIndicator.id.tradeDate)
                .having(technicalIndicator.id.indicatorName.count().eq(expectedCount))
                .fetch();

        Map<String, Set<LocalDate>> result = new HashMap<>();
        for (Tuple row : rows) {
            String code = row.get(technicalIndicator.id.stockCode);
            LocalDate date = row.get(technicalIndicator.id.tradeDate);
            if (code != null && date != null) {
                result.computeIfAbsent(code, k -> new TreeSet<>()).add(date);
            }
        }
        return result;
    }

    /** 특정 종목·지표의 targetDate 직전 가장 최근 계산값 반환. 누적 지표(EMA, OBV 등)의 시드 조회에 사용. */
    public Optional<Double> findLatestValueBefore(MarketType market, String code,
                                                   IndicatorType indicatorType,
                                                   LocalDate beforeDateExclusive) {
        Double value = queryFactory
                .select(technicalIndicator.indicatorValue)
                .from(technicalIndicator)
                .where(
                        technicalIndicator.id.marketType.eq(market),
                        technicalIndicator.id.stockCode.eq(code),
                        technicalIndicator.id.indicatorName.eq(indicatorType),
                        technicalIndicator.id.tradeDate.lt(beforeDateExclusive)
                )
                .orderBy(technicalIndicator.id.tradeDate.desc())
                .limit(1)
                .fetchOne();
        return Optional.ofNullable(value);
    }

    /** 특정 거래일·지표 타입 목록의 전체 종목 시드 벌크 조회. key = stockCode, value = (IndicatorType → 값).
     *  누적 지표(EMA·OBV)의 전날 시드를 종목별 개별 쿼리 없이 한 번에 가져오는 데 사용된다. */
    public Map<String, Map<IndicatorType, Double>> findByMarketAndDateAndTypes(
            MarketType market, LocalDate date, Set<IndicatorType> types) {
        List<Tuple> rows = queryFactory
                .select(
                        technicalIndicator.id.stockCode,
                        technicalIndicator.id.indicatorName,
                        technicalIndicator.indicatorValue
                )
                .from(technicalIndicator)
                .where(
                        technicalIndicator.id.marketType.eq(market),
                        technicalIndicator.id.tradeDate.eq(date),
                        technicalIndicator.id.indicatorName.in(types)
                )
                .fetch();

        Map<String, Map<IndicatorType, Double>> result = new HashMap<>();
        for (Tuple row : rows) {
            String code = row.get(technicalIndicator.id.stockCode);
            IndicatorType type = row.get(technicalIndicator.id.indicatorName);
            Double value = row.get(technicalIndicator.indicatorValue);
            if (code != null && type != null && value != null) {
                result.computeIfAbsent(code, k -> new EnumMap<>(IndicatorType.class)).put(type, value);
            }
        }
        return result;
    }

    /** 특정 종목의 최근 N 거래일 지표 조회. 반환 key = tradeDate, value = (IndicatorType → 값) 맵. */
    public Map<LocalDate, Map<IndicatorType, Double>> findRecentByStock(
            MarketType market, String code, List<IndicatorType> types, int limit) {

        List<LocalDate> dates = queryFactory
                .select(technicalIndicator.id.tradeDate)
                .distinct()
                .from(technicalIndicator)
                .where(
                        technicalIndicator.id.marketType.eq(market),
                        technicalIndicator.id.stockCode.eq(code)
                )
                .orderBy(technicalIndicator.id.tradeDate.desc())
                .limit(limit)
                .fetch();

        if (dates.isEmpty()) return Map.of();

        List<Tuple> rows = queryFactory
                .select(
                        technicalIndicator.id.tradeDate,
                        technicalIndicator.id.indicatorName,
                        technicalIndicator.indicatorValue
                )
                .from(technicalIndicator)
                .where(
                        technicalIndicator.id.marketType.eq(market),
                        technicalIndicator.id.stockCode.eq(code),
                        technicalIndicator.id.tradeDate.in(dates),
                        technicalIndicator.id.indicatorName.in(types)
                )
                .fetch();

        Map<LocalDate, Map<IndicatorType, Double>> result = new TreeMap<>();
        for (Tuple row : rows) {
            LocalDate date = row.get(technicalIndicator.id.tradeDate);
            IndicatorType type = row.get(technicalIndicator.id.indicatorName);
            Double value = row.get(technicalIndicator.indicatorValue);
            if (date != null && type != null && value != null) {
                result.computeIfAbsent(date, k -> new EnumMap<>(IndicatorType.class)).put(type, value);
            }
        }
        return result;
    }
}
