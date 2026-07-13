package com.dove.custommetric.infrastructure.repository;

import com.dove.custommetric.domain.entity.CustomMetricDaily;
import com.dove.custommetric.domain.entity.QCustomMetricDaily;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

/**
 * 커스텀 지표 계산값(CUSTOM_METRIC_DAILY)의 거래일 범위 조회.
 */
@Repository
@RequiredArgsConstructor
public class CustomMetricDailyQuerySupport {

    private final JPAQueryFactory queryFactory;

    /**
     * 지표의 저장값을 거래일 범위(양끝 포함)로 오름차순 조회한다. from/to가 null이면 그 방향은 무제한.
     */
    public List<CustomMetricDaily> findRange(Long metricId, LocalDate from, LocalDate to) {
        QCustomMetricDaily d = QCustomMetricDaily.customMetricDaily;
        BooleanExpression where = d.id.metricId.eq(metricId);
        if (from != null) where = where.and(d.id.tradeDate.goe(from));
        if (to != null) where = where.and(d.id.tradeDate.loe(to));
        return queryFactory.selectFrom(d).where(where).orderBy(d.id.tradeDate.asc()).fetch();
    }
}
