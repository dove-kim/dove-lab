package com.dove.stock.domain.repository;

import com.dove.market.domain.enums.MarketType;
import com.dove.stock.domain.entity.StockListedDate;
import com.dove.stock.domain.entity.StockListedDateId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface StockListedDateRepository extends JpaRepository<StockListedDate, StockListedDateId> {

    List<StockListedDate> findAllById_MarketTypeAndId_Date(MarketType marketType, LocalDate date);

    boolean existsById_MarketTypeAndId_CodeAndId_Date(MarketType marketType, String code, LocalDate date);

    boolean existsById_MarketTypeAndId_Date(MarketType marketType, LocalDate date);

    Optional<StockListedDate> findFirstById_MarketTypeOrderById_DateDesc(MarketType marketType);
}
