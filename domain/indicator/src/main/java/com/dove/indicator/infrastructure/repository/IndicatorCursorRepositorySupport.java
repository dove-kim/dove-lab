package com.dove.indicator.infrastructure.repository;

import com.dove.stock.domain.enums.PriceType;
import com.dove.stock.domain.enums.StockExchange;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static com.dove.indicator.domain.entity.QIndicatorCursor.indicatorCursor;

/**
 * 지표 커서의 QueryDSL 기반 조건부 전진(CAS) 조회 지원.
 */
@Repository
@RequiredArgsConstructor
public class IndicatorCursorRepositorySupport {

    private final JPAQueryFactory queryFactory;

    /**
     * 그룹 커서가 expected와 일치할 때만 toDate로 전진시키고, 갱신된 행 수를 반환한다 (compare-and-set).
     * 0이면 그 사이 rewind/삭제로 값이 달라진 것이다.
     */
    public long advanceIfMatches(String ticker, StockExchange exchange, PriceType priceType,
                                 LocalDate expected, LocalDate toDate) {
        BooleanExpression where = indicatorCursor.ticker.eq(ticker)
                .and(indicatorCursor.exchange.eq(exchange))
                .and(indicatorCursor.priceType.eq(priceType));
        where = (expected == null)
                ? where.and(indicatorCursor.cursorDate.isNull())
                : where.and(indicatorCursor.cursorDate.eq(expected));

        return queryFactory.update(indicatorCursor)
                .set(indicatorCursor.cursorDate, toDate)
                .set(indicatorCursor.updatedAt, LocalDateTime.now())
                .where(where)
                .execute();
    }

    /**
     * 거래소 내 cursorDate가 target보다 뒤인 모든 커서를 target으로 일괄 하향한다(가격유형 무관).
     * 갱신된 행 수를 반환한다(되돌릴 게 없으면 0). 종목 수만큼의 단건 갱신을 한 문장으로 대체한다.
     */
    public long rewindExchangeBefore(StockExchange exchange, LocalDate target) {
        return queryFactory.update(indicatorCursor)
                .set(indicatorCursor.cursorDate, target)
                .set(indicatorCursor.updatedAt, LocalDateTime.now())
                .where(indicatorCursor.exchange.eq(exchange)
                        .and(indicatorCursor.cursorDate.gt(target)))
                .execute();
    }
}
