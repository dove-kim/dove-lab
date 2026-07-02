package com.dove.indicator.domain.breadth.repository;

import com.dove.indicator.domain.breadth.entity.StockBreadthDaily;
import com.dove.indicator.domain.breadth.entity.StockBreadthDailyId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * 당일 상승비율 행(STOCK_BREADTH_DAILY) 저장소.
 */
@Repository
public interface StockBreadthDailyRepository
        extends JpaRepository<StockBreadthDaily, StockBreadthDailyId> {
}
