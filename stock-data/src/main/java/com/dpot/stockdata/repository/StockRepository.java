package com.dpot.stockdata.repository;

import com.dpot.stockdata.repository.entity.Stock;
import com.dpot.stockdata.repository.entity.StockId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StockRepository extends JpaRepository<Stock, StockId> {
}
