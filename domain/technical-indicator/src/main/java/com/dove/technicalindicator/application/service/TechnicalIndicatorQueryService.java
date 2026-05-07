package com.dove.technicalindicator.application.service;

import com.dove.market.domain.enums.MarketType;
import com.dove.technicalindicator.domain.enums.IndicatorType;
import com.dove.technicalindicator.infrastructure.repository.TechnicalIndicatorQueryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TechnicalIndicatorQueryService {

    private final TechnicalIndicatorQueryRepository technicalIndicatorQueryRepository;

    public Map<String, Map<IndicatorType, Double>> findAllByMarketsAndDate(List<MarketType> markets, LocalDate date) {
        return technicalIndicatorQueryRepository.findAllByMarketsAndDate(markets, date);
    }

    public Map<LocalDate, Map<IndicatorType, Double>> findRecentByStock(
            MarketType market, String code, List<IndicatorType> types, int limit) {
        return technicalIndicatorQueryRepository.findRecentByStock(market, code, types, limit);
    }
}
