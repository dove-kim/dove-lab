package com.dove.stockdata.domain.repository;

import com.dove.stockdata.domain.entity.StockData;
import com.dove.stockdata.domain.entity.StockDataId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/** 일별 주가 데이터 저장 리포지토리. */
@Repository
public interface StockDataRepository extends JpaRepository<StockData, StockDataId> {
}
