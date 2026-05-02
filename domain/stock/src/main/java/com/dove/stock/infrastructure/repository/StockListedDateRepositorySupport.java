package com.dove.stock.infrastructure.repository;

import com.dove.market.domain.enums.MarketType;
import com.querydsl.jpa.impl.JPAQueryFactory;

import static com.dove.stock.domain.entity.QStockListedDate.stockListedDate;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class StockListedDateRepositorySupport {

    private final JPAQueryFactory queryFactory;

    public List<LocalDate> findDistinctDatesInRange(MarketType marketType, LocalDate from, LocalDate to) {
        return queryFactory
                .selectDistinct(stockListedDate.id.date)
                .from(stockListedDate)
                .where(
                        stockListedDate.id.marketType.eq(marketType),
                        stockListedDate.id.date.between(from, to)
                )
                .fetch();
    }
}
