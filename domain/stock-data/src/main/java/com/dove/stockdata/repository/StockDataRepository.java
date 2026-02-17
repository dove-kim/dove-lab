package com.dove.stockdata.repository;

import com.dove.stockdata.entity.StockData;
import com.dove.stockdata.entity.StockDataId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StockDataRepository extends JpaRepository<StockData, StockDataId> {
}
