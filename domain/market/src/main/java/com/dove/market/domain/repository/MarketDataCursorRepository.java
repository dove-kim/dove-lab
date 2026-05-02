package com.dove.market.domain.repository;

import com.dove.market.domain.entity.MarketDataCursor;
import com.dove.market.domain.enums.MarketType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MarketDataCursorRepository extends JpaRepository<MarketDataCursor, MarketType> {
}
