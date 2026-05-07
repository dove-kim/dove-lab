package com.dove.technicalindicator.infrastructure.repository;

import com.dove.market.domain.enums.MarketType;
import com.dove.technicalindicator.domain.enums.IndicatorType;

import static com.dove.technicalindicator.domain.entity.QTechnicalIndicator.technicalIndicator;
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
import java.util.TreeMap;

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
