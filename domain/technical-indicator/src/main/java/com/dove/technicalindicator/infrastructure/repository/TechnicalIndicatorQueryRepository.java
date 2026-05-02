package com.dove.technicalindicator.infrastructure.repository;

import com.dove.market.domain.enums.MarketType;
import com.dove.technicalindicator.domain.enums.IndicatorType;

import static com.dove.technicalindicator.domain.entity.QTechnicalIndicator.technicalIndicator;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

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
}
