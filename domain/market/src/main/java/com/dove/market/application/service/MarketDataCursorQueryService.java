package com.dove.market.application.service;

import com.dove.market.domain.entity.MarketDataCursor;
import com.dove.market.domain.enums.MarketType;
import com.dove.market.domain.repository.MarketDataCursorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class MarketDataCursorQueryService {

    private final MarketDataCursorRepository marketDataCursorRepository;

    @Transactional(readOnly = true)
    public Optional<LocalDate> findLastProcessedDate(MarketType market) {
        return marketDataCursorRepository.findById(market)
                .map(MarketDataCursor::getLastProcessedDate);
    }
}
