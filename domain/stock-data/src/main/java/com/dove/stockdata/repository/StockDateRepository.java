package com.dove.stockdata.repository;

import com.dove.stockdata.entity.StockDate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;

@Repository
public interface StockDateRepository extends JpaRepository<StockDate, LocalDate> {
}
