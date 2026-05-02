package com.dove.market.application.service;

import com.dove.market.domain.entity.MarketDataCursor;
import com.dove.market.domain.enums.MarketType;
import com.dove.market.domain.repository.MarketDataCursorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class MarketDataCursorCommandService {

    private final MarketDataCursorRepository marketDataCursorRepository;

    @Transactional
    public void upsert(MarketType market, LocalDate date) {
        marketDataCursorRepository.findById(market)
                .ifPresentOrElse(
                        cursor -> cursor.updateLastProcessedDate(date),
                        () -> marketDataCursorRepository.save(new MarketDataCursor(market, date))
                );
    }
}
