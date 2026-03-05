package com.dove.stockdata.domain.repository;

import com.dove.stockdata.domain.entity.MarketCalendar;
import com.dove.stockdata.domain.entity.MarketCalendarId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/** 시장 캘린더 리포지토리. */
@Repository
public interface MarketCalendarRepository extends JpaRepository<MarketCalendar, MarketCalendarId> {
}
