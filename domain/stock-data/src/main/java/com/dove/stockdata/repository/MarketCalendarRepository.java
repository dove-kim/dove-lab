package com.dove.stockdata.repository;

import com.dove.stockdata.entity.MarketCalendar;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;

@Repository
public interface MarketCalendarRepository extends JpaRepository<MarketCalendar, LocalDate> {
}
