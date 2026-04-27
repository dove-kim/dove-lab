package com.dove.market.application.service;

import com.dove.market.domain.entity.MarketCalendar;
import com.dove.market.domain.entity.MarketCalendarId;
import com.dove.market.domain.enums.MarketDayType;
import com.dove.market.domain.enums.MarketType;
import com.dove.market.domain.repository.MarketCalendarRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Optional;

/** 시장 캘린더 조회 전용 서비스. */
@Service
@RequiredArgsConstructor
public class MarketCalendarQueryService {

    private final MarketCalendarRepository marketCalendarRepository;

    @Transactional(readOnly = true)
    public Optional<MarketCalendar> findById(LocalDate date, MarketType marketType) {
        return marketCalendarRepository.findById(new MarketCalendarId(date, marketType));
    }

    @Transactional(readOnly = true)
    public Optional<MarketCalendar> findLatestTrading(MarketType marketType) {
        return marketCalendarRepository.findFirstByMarketTypeAndDayTypeOrderByDateDesc(
                marketType, MarketDayType.TRADING);
    }
}
