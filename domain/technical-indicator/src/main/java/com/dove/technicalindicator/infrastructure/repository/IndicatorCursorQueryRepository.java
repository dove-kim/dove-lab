package com.dove.technicalindicator.infrastructure.repository;

import com.dove.market.domain.enums.MarketType;
import com.dove.stock.domain.enums.TradingStatus;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

import static com.dove.stock.domain.entity.QStock.stock;
import static com.dove.technicalindicator.domain.entity.QIndicatorCursor.indicatorCursor;

@Repository
@RequiredArgsConstructor
public class IndicatorCursorQueryRepository {

    private final JPAQueryFactory queryFactory;

    /** 지표 계산 대상 종목 코드 반환. ACTIVE/SUSPENDED 전체 조회 — cursor 유무·최신 여부 무관. */
    public List<String> findEligibleStockCodes(MarketType market) {
        return queryFactory
                .select(stock.id.code)
                .from(stock)
                .where(
                        stock.id.marketType.eq(market),
                        stock.tradingStatus.in(TradingStatus.ACTIVE, TradingStatus.SUSPENDED)
                )
                .fetch();
    }

    /** lastCalculatedDate < targetDate 인 종목 수. 0이면 해당 시장 지표 계산 완료. */
    public long countLaggingCodes(MarketType market, LocalDate targetDate) {
        Long count = queryFactory
                .select(indicatorCursor.id.code.countDistinct())
                .from(indicatorCursor)
                .where(
                        indicatorCursor.id.marketType.eq(market),
                        indicatorCursor.lastCalculatedDate.lt(targetDate)
                )
                .fetchOne();
        return count != null ? count : 0L;
    }
}
