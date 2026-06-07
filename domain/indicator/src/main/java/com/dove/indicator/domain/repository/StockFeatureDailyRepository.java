package com.dove.indicator.domain.repository;

import com.dove.indicator.domain.entity.StockFeatureDaily;
import com.dove.indicator.domain.entity.StockFeatureDailyId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * wide 피처 행(STOCK_FEATURE_DAILY) 저장소.
 */
@Repository
public interface StockFeatureDailyRepository
        extends JpaRepository<StockFeatureDaily, StockFeatureDailyId> {
}
