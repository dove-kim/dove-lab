package com.dove.indicator.infrastructure.repository;

import com.dove.stock.domain.enums.MarketUniverse;
import com.dove.stock.domain.enums.PriceType;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static com.dove.indicator.domain.rank.entity.QRankCursor.rankCursor;

/**
 * 순위 커서의 QueryDSL 기반 조건부 전진(CAS) 조회 지원.
 */
@Repository
@RequiredArgsConstructor
public class RankCursorRepositorySupport {

    private final JPAQueryFactory queryFactory;

    /**
     * universe 커서가 expected와 일치할 때만 toDate로 전진시키고, 갱신된 행 수를 반환한다 (compare-and-set).
     * 0이면 그 사이 값이 달라진 것이다.
     */
    public long advanceIfMatches(MarketUniverse universe, PriceType priceType,
                                 LocalDate expected, LocalDate toDate) {
        BooleanExpression where = rankCursor.universe.eq(universe)
                .and(rankCursor.priceType.eq(priceType));
        where = (expected == null)
                ? where.and(rankCursor.cursorDate.isNull())
                : where.and(rankCursor.cursorDate.eq(expected));

        return queryFactory.update(rankCursor)
                .set(rankCursor.cursorDate, toDate)
                .set(rankCursor.updatedAt, LocalDateTime.now())
                .where(where)
                .execute();
    }
}
