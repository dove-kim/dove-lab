package com.dpot.stockdata.repository;

import com.dpot.stockdata.repository.entity.StockData;
import com.dpot.stockdata.repository.entity.StockDataId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StockDataRepository extends JpaRepository<StockData, StockDataId> {
}
