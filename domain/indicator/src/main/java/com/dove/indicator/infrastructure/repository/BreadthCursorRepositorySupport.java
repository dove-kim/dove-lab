package com.dove.indicator.infrastructure.repository;

import com.dove.stock.domain.enums.MarketUniverse;
import com.dove.stock.domain.enums.PriceType;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static com.dove.indicator.domain.breadth.entity.QBreadthCursor.breadthCursor;

/**
 * 상승비율 커서의 QueryDSL 기반 조건부 전진(CAS) 조회 지원.
 */
@Repository
@RequiredArgsConstructor
public class BreadthCursorRepositorySupport {

    private final JPAQueryFactory queryFactory;

    /**
     * universe 커서가 expected와 일치할 때만 toDate로 전진시키고, 갱신된 행 수를 반환한다 (compare-and-set).
     * 0이면 그 사이 값이 달라진 것이다.
     */
    public long advanceIfMatches(MarketUniverse universe, PriceType priceType,
                                 LocalDate expected, LocalDate toDate) {
        BooleanExpression where = breadthCursor.universe.eq(universe)
                .and(breadthCursor.priceType.eq(priceType));
        where = (expected == null)
                ? where.and(breadthCursor.cursorDate.isNull())
                : where.and(breadthCursor.cursorDate.eq(expected));

        return queryFactory.update(breadthCursor)
                .set(breadthCursor.cursorDate, toDate)
                .set(breadthCursor.updatedAt, LocalDateTime.now())
                .where(where)
                .execute();
    }
}
