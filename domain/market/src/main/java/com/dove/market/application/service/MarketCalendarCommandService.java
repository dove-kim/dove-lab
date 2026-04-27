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

/** 시장 캘린더 변경 전용 서비스. */
@Service
@RequiredArgsConstructor
public class MarketCalendarCommandService {

    private final MarketCalendarRepository marketCalendarRepository;

    @Transactional
    public void upsertDayType(LocalDate date, MarketType marketType, MarketDayType dayType) {
        marketCalendarRepository.findById(new MarketCalendarId(date, marketType))
                .ifPresentOrElse(
                        existing -> existing.updateDayType(dayType),
                        () -> marketCalendarRepository.save(new MarketCalendar(date, marketType, dayType))
                );
    }
}
