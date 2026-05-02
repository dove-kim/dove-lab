package com.dove.stockprice.domain.repository;

import com.dove.market.domain.enums.MarketType;
import com.dove.stockprice.domain.entity.DailyStockPrice;
import com.dove.stockprice.domain.entity.DailyStockPriceId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface DailyStockPriceRepository extends JpaRepository<DailyStockPrice, DailyStockPriceId> {

    boolean existsById_MarketTypeAndId_TradeDate(MarketType marketType, LocalDate tradeDate);

    Optional<DailyStockPrice> findFirstById_MarketTypeOrderById_TradeDateDesc(MarketType marketType);
}
