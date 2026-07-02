package com.dove.modelserving.domain.repository;

import com.dove.modelserving.domain.entity.StockModelScore;
import com.dove.modelserving.domain.entity.StockModelScoreId;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 모델 채점 점수 CRUD 저장소.
 */
public interface StockModelScoreRepository extends JpaRepository<StockModelScore, StockModelScoreId> {
}
