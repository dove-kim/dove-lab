package com.dove.stock.domain.repository;

import com.dove.market.domain.enums.MarketType;
import com.dove.stock.domain.entity.Stock;
import com.dove.stock.domain.entity.StockId;
import com.dove.stock.domain.enums.TradingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StockRepository extends JpaRepository<Stock, StockId> {
    Optional<Stock> findById_MarketTypeAndId_Code(MarketType marketType, String code);

    default Optional<Stock> findByMarketTypeAndCode(MarketType marketType, String code) {
        return findById_MarketTypeAndId_Code(marketType, code);
    }

    List<Stock> findAllByTradingStatus(TradingStatus tradingStatus);

    List<Stock> findAllByTradingStatusAndId_MarketType(TradingStatus tradingStatus, MarketType marketType);

    List<Stock> findAllById_MarketType(MarketType marketType);
}
