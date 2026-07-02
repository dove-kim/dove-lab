package com.dove.modelserving.infrastructure.repository;

import com.dove.modelserving.domain.entity.StockModelScore;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

import static com.dove.modelserving.domain.entity.QStockModelScore.stockModelScore;

/**
 * 모델 채점 점수의 QueryDSL 기반 조회·삭제 지원.
 */
@Repository
@RequiredArgsConstructor
public class StockModelScoreRepositorySupport {

    private final JPAQueryFactory queryFactory;

    /**
     * 한 모델·종목의 점수를 거래일 오름차순으로 반환한다(차트용).
     */
    public List<StockModelScore> findByModelAndTicker(Long modelId, String ticker) {
        return findByModelTickerAndDateRange(modelId, ticker, null, null);
    }

    /**
     * 한 모델·종목의 거래일 구간[from, to] 점수를 거래일 오름차순으로 반환한다(차트용). 경계가 null이면 무제한.
     */
    public List<StockModelScore> findByModelTickerAndDateRange(Long modelId, String ticker,
                                                               LocalDate from, LocalDate to) {
        BooleanExpression where = stockModelScore.id.modelId.eq(modelId)
                .and(stockModelScore.id.ticker.eq(ticker));
        if (from != null) where = where.and(stockModelScore.id.tradeDate.goe(from));
        if (to != null) where = where.and(stockModelScore.id.tradeDate.loe(to));
        return queryFactory.selectFrom(stockModelScore)
                .where(where)
                .orderBy(stockModelScore.id.tradeDate.asc())
                .fetch();
    }

    /**
     * 한 모델·거래일의 전 종목 점수를 반환한다(인메모리 폴백 평가용).
     */
    public List<StockModelScore> findByModelAndDate(Long modelId, LocalDate date) {
        return queryFactory.selectFrom(stockModelScore)
                .where(stockModelScore.id.modelId.eq(modelId),
                        stockModelScore.id.tradeDate.eq(date))
                .fetch();
    }

    /**
     * 한 모델의 모든 점수를 삭제하고 삭제된 행 수를 반환한다.
     */
    public long deleteByModel(Long modelId) {
        return queryFactory.delete(stockModelScore)
                .where(stockModelScore.id.modelId.eq(modelId))
                .execute();
    }

    /**
     * 한 모델에서 거래일 구간[from, to]의 점수를 삭제하고 삭제된 행 수를 반환한다. 경계는 null이면 무제한.
     */
    public long deleteByModelAndDateRange(Long modelId, LocalDate from, LocalDate to) {
        BooleanExpression where = stockModelScore.id.modelId.eq(modelId);
        if (from != null) where = where.and(stockModelScore.id.tradeDate.goe(from));
        if (to != null) where = where.and(stockModelScore.id.tradeDate.loe(to));
        return queryFactory.delete(stockModelScore)
                .where(where)
                .execute();
    }

    /**
     * 한 모델·종목의 점수를 삭제하고 삭제된 행 수를 반환한다.
     */
    public long deleteByModelAndTicker(Long modelId, String ticker) {
        return queryFactory.delete(stockModelScore)
                .where(stockModelScore.id.modelId.eq(modelId),
                        stockModelScore.id.ticker.eq(ticker))
                .execute();
    }
}
