package com.dove.technicalindicator.application.service;

import com.dove.market.domain.enums.MarketType;
import com.dove.technicalindicator.domain.entity.IndicatorCursor;
import com.dove.technicalindicator.domain.repository.IndicatorCursorRepository;
import com.dove.technicalindicator.infrastructure.repository.IndicatorCursorQueryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class IndicatorCursorQueryService {

    private final IndicatorCursorRepository indicatorCursorRepository;
    private final IndicatorCursorQueryRepository indicatorCursorQueryRepository;

    @Transactional(readOnly = true)
    public List<IndicatorCursor> findAllByMarketAndCode(MarketType market, String code) {
        return indicatorCursorRepository.findAllById_MarketTypeAndId_Code(market, code);
    }

    @Transactional(readOnly = true)
    public List<String> findEligibleStockCodes(MarketType market) {
        return indicatorCursorQueryRepository.findEligibleStockCodes(market);
    }

    @Transactional(readOnly = true)
    public long countLagging(MarketType market, LocalDate targetDate) {
        return indicatorCursorQueryRepository.countLaggingCodes(market, targetDate);
    }
}
