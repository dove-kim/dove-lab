package com.dove.stockdata.domain.repository;

import com.dove.stockdata.domain.entity.Stock;
import com.dove.stockdata.domain.entity.StockId;
import com.dove.stockdata.domain.enums.MarketType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/** 종목 마스터 리포지토리. */
@Repository
public interface StockRepository extends JpaRepository<Stock, StockId> {
    Optional<Stock> findById_MarketTypeAndId_Code(MarketType marketType, String code);

    default Optional<Stock> findByMarketTypeAndCode(MarketType marketType, String code) {
        return findById_MarketTypeAndId_Code(marketType, code);
    }
}
