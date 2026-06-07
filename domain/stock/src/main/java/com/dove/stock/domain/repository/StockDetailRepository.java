package com.dove.stock.domain.repository;

import com.dove.stock.domain.entity.StockDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * StockDetail 엔티티 저장소.
 */
@Repository
public interface StockDetailRepository extends JpaRepository<StockDetail, String> {
}
