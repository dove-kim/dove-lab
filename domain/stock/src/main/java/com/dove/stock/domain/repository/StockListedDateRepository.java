package com.dove.stock.domain.repository;

import com.dove.market.domain.enums.MarketType;
import com.dove.stock.domain.entity.StockListedDate;
import com.dove.stock.domain.entity.StockListedDateId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

/** 종목 상장 이력 리포지토리. */
@Repository
public interface StockListedDateRepository extends JpaRepository<StockListedDate, StockListedDateId> {

    List<StockListedDate> findAllById_MarketTypeAndId_Date(MarketType marketType, LocalDate date);
}
