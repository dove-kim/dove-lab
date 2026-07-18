package com.dove.stock.domain.repository;

import com.dove.stock.domain.entity.StockDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * StockDetail 엔티티 저장소.
 */
@Repository
public interface StockDetailRepository extends JpaRepository<StockDetail, String> {

    /**
     * 상품약명·상품명 부분일치 또는 티커 접두일치로 종목을 검색한다(자동완성용, 최대 20건).
     */
    List<StockDetail> findTop20ByPrdtAbrvNameContainingIgnoreCaseOrPrdtNameContainingIgnoreCaseOrTickerStartingWith(
            String abrvName, String name, String ticker);
}
