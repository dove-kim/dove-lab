package com.dove.modelserving.domain.repository;

import com.dove.modelserving.domain.entity.StockModelScore;
import com.dove.modelserving.domain.entity.StockModelScoreId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;

/**
 * 모델 채점 점수 CRUD 저장소.
 */
public interface StockModelScoreRepository extends JpaRepository<StockModelScore, StockModelScoreId> {

    /**
     * 한 모델의 특정 거래일 점수를 모두 삭제한다.
     */
    void deleteByIdModelIdAndIdTradeDate(Long modelId, LocalDate tradeDate);
}
