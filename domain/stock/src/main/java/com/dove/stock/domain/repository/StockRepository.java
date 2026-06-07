package com.dove.stock.domain.repository;

import com.dove.market.domain.enums.MarketType;
import com.dove.stock.domain.entity.Stock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Stock 엔티티 저장소.
 */
@Repository
public interface StockRepository extends JpaRepository<Stock, String> {
    List<Stock> findByMarket(MarketType market);
}
