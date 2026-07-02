package com.dove.indicator.domain.rank.repository;

import com.dove.indicator.domain.rank.entity.StockRankDaily;
import com.dove.indicator.domain.rank.entity.StockRankDailyId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * wide 순위 행(STOCK_RANK_DAILY) 저장소.
 */
@Repository
public interface StockRankDailyRepository
        extends JpaRepository<StockRankDaily, StockRankDailyId> {
}
