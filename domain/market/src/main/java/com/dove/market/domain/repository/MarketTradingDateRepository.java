package com.dove.market.domain.repository;

import com.dove.market.domain.entity.MarketTradingDate;
import com.dove.market.domain.entity.MarketTradingDateId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MarketTradingDateRepository extends JpaRepository<MarketTradingDate, MarketTradingDateId> {
}
