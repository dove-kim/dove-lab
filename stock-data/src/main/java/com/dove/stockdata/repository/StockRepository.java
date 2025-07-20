package com.dove.stockdata.repository;

import com.dove.stockdata.entity.Stock;
import com.dove.stockdata.entity.StockId;
import com.dove.stockdata.enums.MarketType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface StockRepository extends JpaRepository<Stock, StockId> {
    Optional<Stock> findById_MarketTypeAndId_Code(MarketType marketType, String code);
}
