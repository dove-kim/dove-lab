package com.dove.market.domain.repository;

import com.dove.market.domain.entity.MarketCalendar;
import com.dove.market.domain.entity.MarketCalendarId;
import com.dove.market.domain.enums.MarketDayType;
import com.dove.market.domain.enums.MarketType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/** 시장 캘린더 리포지토리. */
@Repository
public interface MarketCalendarRepository extends JpaRepository<MarketCalendar, MarketCalendarId> {

    Optional<MarketCalendar> findFirstByMarketTypeAndDayTypeOrderByDateDesc(
            MarketType marketType, MarketDayType dayType);
}
