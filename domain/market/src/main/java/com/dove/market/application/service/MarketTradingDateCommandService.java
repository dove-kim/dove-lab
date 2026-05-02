package com.dove.market.application.service;

import com.dove.market.domain.entity.MarketTradingDate;
import com.dove.market.domain.entity.MarketTradingDateId;
import com.dove.market.domain.enums.MarketType;
import com.dove.market.domain.repository.MarketTradingDateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class MarketTradingDateCommandService {

    private final MarketTradingDateRepository marketTradingDateRepository;

    /** closed → open 방향 업데이트만 허용. 한번 개장으로 확정된 날짜는 되돌리지 않는다. */
    @Transactional
    public void upsert(MarketType market, LocalDate date, boolean isOpen) {
        MarketTradingDateId id = new MarketTradingDateId(market, date);
        marketTradingDateRepository.findById(id).ifPresentOrElse(
                existing -> {
                    if (!existing.isOpen() && isOpen) {
                        existing.markOpen();
                    }
                },
                () -> marketTradingDateRepository.save(new MarketTradingDate(id, isOpen))
        );
    }
}
