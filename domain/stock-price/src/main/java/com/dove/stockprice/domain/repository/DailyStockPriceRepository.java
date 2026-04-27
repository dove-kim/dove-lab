package com.dove.stockprice.domain.repository;

import com.dove.stockprice.domain.entity.DailyStockPrice;
import com.dove.stockprice.domain.entity.DailyStockPriceId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/** 일별 주가 데이터 저장 리포지토리. */
@Repository
public interface DailyStockPriceRepository extends JpaRepository<DailyStockPrice, DailyStockPriceId> {
}
