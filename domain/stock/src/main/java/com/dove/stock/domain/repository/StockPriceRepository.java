package com.dove.stock.domain.repository;

import com.dove.stock.domain.entity.StockPrice;
import com.dove.stock.domain.entity.StockPriceId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * StockPrice 엔티티 저장소.
 */
@Repository
public interface StockPriceRepository extends JpaRepository<StockPrice, StockPriceId> {
}
