package com.dove.stock.domain.repository;

import com.dove.market.domain.enums.MarketType;
import com.dove.stock.domain.entity.Stock;
import com.dove.stock.domain.entity.StockId;
import com.dove.stock.domain.enums.TradingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/** 종목 마스터 리포지토리. */
@Repository
public interface StockRepository extends JpaRepository<Stock, StockId> {
    Optional<Stock> findById_MarketTypeAndId_Code(MarketType marketType, String code);

    default Optional<Stock> findByMarketTypeAndCode(MarketType marketType, String code) {
        return findById_MarketTypeAndId_Code(marketType, code);
    }

    /** 현재 거래 상태가 지정 값인 종목 전체. */
    List<Stock> findAllByTradingStatus(TradingStatus tradingStatus);

    /** 시장별 거래 상태 필터. */
    List<Stock> findAllByTradingStatusAndId_MarketType(TradingStatus tradingStatus, MarketType marketType);

    /** 시장별 종목 전체. */
    List<Stock> findAllById_MarketType(MarketType marketType);
}
