package com.dove.modelserving.infrastructure.repository;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static com.dove.modelserving.domain.entity.QMlModel.mlModel;

/**
 * 모델 채점 커서의 QueryDSL 기반 조건부 전진(CAS) 지원.
 */
@Repository
@RequiredArgsConstructor
public class MlModelRepositorySupport {

    private final JPAQueryFactory queryFactory;

    /**
     * 모델의 scoreCursor가 expected와 일치할 때만 toDate로 전진시키고, 갱신된 행 수를 반환한다(compare-and-set).
     * 0이면 그 사이 커서가 달라진 것이다.
     */
    public long advanceScoreCursorIfMatches(Long modelId, LocalDate expected, LocalDate toDate) {
        BooleanExpression where = mlModel.id.eq(modelId);
        where = (expected == null)
                ? where.and(mlModel.scoreCursor.isNull())
                : where.and(mlModel.scoreCursor.eq(expected));

        return queryFactory.update(mlModel)
                .set(mlModel.scoreCursor, toDate)
                .set(mlModel.updatedAt, LocalDateTime.now())
                .where(where)
                .execute();
    }
}
