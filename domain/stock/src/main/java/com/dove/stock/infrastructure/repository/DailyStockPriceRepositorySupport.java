package com.dove.stock.infrastructure.repository;

import com.dove.market.domain.enums.MarketType;
import com.dove.stock.domain.entity.DailyStockPrice;
import com.querydsl.core.Tuple;
import com.querydsl.jpa.impl.JPAQueryFactory;

import static com.dove.stock.domain.entity.QDailyStockPrice.dailyStockPrice;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class DailyStockPriceRepositorySupport {

    private final JPAQueryFactory queryFactory;

    public List<DailyStockPrice> findRecentDailyStockPrice(MarketType marketType, String stockCode,
                                                           LocalDate tradeDate, int limit) {
        return queryFactory
                .selectFrom(dailyStockPrice)
                .where(
                        dailyStockPrice.id.marketType.eq(marketType),
                        dailyStockPrice.id.stockCode.eq(stockCode),
                        dailyStockPrice.id.tradeDate.loe(tradeDate)
                )
                .orderBy(dailyStockPrice.id.tradeDate.desc())
                .limit(limit)
                .fetch();
    }

    public List<String> findStockCodesByMarketTypeAndTradeDate(MarketType marketType, LocalDate tradeDate) {
        return queryFactory
                .selectDistinct(dailyStockPrice.id.stockCode)
                .from(dailyStockPrice)
                .where(
                        dailyStockPrice.id.marketType.eq(marketType),
                        dailyStockPrice.id.tradeDate.eq(tradeDate)
                )
                .fetch();
    }

    public List<LocalDate> findDistinctTradeDatesInRange(MarketType marketType, LocalDate from, LocalDate to) {
        return queryFactory
                .selectDistinct(dailyStockPrice.id.tradeDate)
                .from(dailyStockPrice)
                .where(
                        dailyStockPrice.id.marketType.eq(marketType),
                        dailyStockPrice.id.tradeDate.between(from, to)
                )
                .fetch();
    }

    /** 선택된 시장들의 데이터 중 기준일 기준으로 N번째 이전 거래일을 반환.
     *  inclusive=true  → 기준일 당일 포함 (≤), offset=0 이면 SAME_DAY
     *  inclusive=false → 기준일 제외 (<),  offset=0 이면 PREV_1D */
    public Optional<LocalDate> findNthRecentTradeDate(List<MarketType> markets,
                                                       LocalDate reference,
                                                       boolean inclusive,
                                                       long offset) {
        return Optional.ofNullable(
                queryFactory
                        .selectDistinct(dailyStockPrice.id.tradeDate)
                        .from(dailyStockPrice)
                        .where(
                                dailyStockPrice.id.marketType.in(markets),
                                inclusive
                                        ? dailyStockPrice.id.tradeDate.loe(reference)
                                        : dailyStockPrice.id.tradeDate.lt(reference)
                        )
                        .orderBy(dailyStockPrice.id.tradeDate.desc())
                        .offset(offset)
                        .limit(1)
                        .fetchOne()
        );
    }

    /** 선택된 시장들에서 데이터가 있는 최신 거래일 반환. */
    public Optional<LocalDate> findLatestTradeDate(List<MarketType> markets) {
        return Optional.ofNullable(
                queryFactory
                        .select(dailyStockPrice.id.tradeDate.max())
                        .from(dailyStockPrice)
                        .where(dailyStockPrice.id.marketType.in(markets))
                        .fetchOne()
        );
    }

    /** 선택된 시장들에서 to 날짜 이하 최근 limit개 거래일 반환 (내림차순). */
    public List<LocalDate> findRecentTradeDates(List<MarketType> markets, LocalDate to, int limit) {
        return queryFactory
                .selectDistinct(dailyStockPrice.id.tradeDate)
                .from(dailyStockPrice)
                .where(
                        dailyStockPrice.id.marketType.in(markets),
                        dailyStockPrice.id.tradeDate.loe(to)
                )
                .orderBy(dailyStockPrice.id.tradeDate.desc())
                .limit(limit)
                .fetch();
    }

    /** 시장·기간 내 종목별 거래일 집합 반환. key = stockCode, value = 오름차순 거래일 집합.
     *  스캐너의 지표 누락 검사에 사용된다. */
    public Map<String, Set<LocalDate>> findPriceDatesByStock(MarketType marketType, LocalDate from, LocalDate to) {
        List<Tuple> rows = queryFactory
                .select(dailyStockPrice.id.stockCode, dailyStockPrice.id.tradeDate)
                .from(dailyStockPrice)
                .where(
                        dailyStockPrice.id.marketType.eq(marketType),
                        dailyStockPrice.id.tradeDate.between(from, to)
                )
                .fetch();

        Map<String, Set<LocalDate>> result = new HashMap<>();
        for (Tuple row : rows) {
            String code = row.get(dailyStockPrice.id.stockCode);
            LocalDate date = row.get(dailyStockPrice.id.tradeDate);
            if (code != null && date != null) {
                result.computeIfAbsent(code, k -> new TreeSet<>()).add(date);
            }
        }
        return result;
    }

    /** 종목별 전체 주가 오름차순 조회. IndicatorBulkCalculateService 의 in-memory 슬라이딩 윈도우에 사용된다. */
    public List<DailyStockPrice> findAllByMarketAndCode(MarketType marketType, String stockCode) {
        return queryFactory
                .selectFrom(dailyStockPrice)
                .where(
                        dailyStockPrice.id.marketType.eq(marketType),
                        dailyStockPrice.id.stockCode.eq(stockCode)
                )
                .orderBy(dailyStockPrice.id.tradeDate.asc())
                .fetch();
    }

    /** 특정 종목·날짜 목록의 주가 조회. key = tradeDate. */
    public Map<LocalDate, DailyStockPrice> findByMarketAndCodeAndDates(
            MarketType marketType, String stockCode, List<LocalDate> dates) {
        return queryFactory
                .selectFrom(dailyStockPrice)
                .where(
                        dailyStockPrice.id.marketType.eq(marketType),
                        dailyStockPrice.id.stockCode.eq(stockCode),
                        dailyStockPrice.id.tradeDate.in(dates)
                )
                .fetch()
                .stream()
                .collect(Collectors.toMap(p -> p.getId().getTradeDate(), p -> p));
    }

    /** 특정 거래일의 시장별 전체 주가 벌크 조회. key = stockCode. */
    public Map<String, DailyStockPrice> findAllByMarketsAndDate(List<MarketType> markets, LocalDate date) {
        return queryFactory
                .selectFrom(dailyStockPrice)
                .where(
                        dailyStockPrice.id.marketType.in(markets),
                        dailyStockPrice.id.tradeDate.eq(date)
                )
                .fetch()
                .stream()
                .collect(Collectors.toMap(p -> p.getId().getStockCode(), p -> p));
    }
}
