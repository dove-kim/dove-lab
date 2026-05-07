package com.dove.stockprice.infrastructure.repository;

import com.dove.market.domain.enums.MarketType;
import com.dove.stockprice.domain.entity.DailyStockPrice;
import com.querydsl.jpa.impl.JPAQueryFactory;

import static com.dove.stockprice.domain.entity.QDailyStockPrice.dailyStockPrice;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
