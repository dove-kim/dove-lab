package com.dove.stock.domain.repository;

import com.dove.stock.domain.entity.StockTagValue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * StockTagValue 엔티티 저장소.
 */
@Repository
public interface StockTagValueRepository extends JpaRepository<StockTagValue, Long> {

    boolean existsByFieldAndValue(String field, String value);

    List<StockTagValue> findAllByOrderByFieldAscValueAsc();
}
