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
public class MarketTradingDateQueryService {

    private final MarketTradingDateRepository marketTradingDateRepository;

    @Transactional(readOnly = true)
    public boolean existsOpenDay(MarketType market, LocalDate date) {
        return marketTradingDateRepository.findById(new MarketTradingDateId(market, date))
                .map(MarketTradingDate::isOpen)
                .orElse(false);
    }
}
