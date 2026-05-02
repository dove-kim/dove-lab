package com.dove.stock.application.service;

import com.dove.market.domain.enums.MarketType;
import com.dove.stock.domain.entity.StockListedDate;
import com.dove.stock.domain.repository.StockListedDateRepository;
import com.dove.stock.infrastructure.repository.StockListedDateRepositorySupport;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StockListedDateQueryService {

    private final StockListedDateRepository stockListedDateRepository;
    private final StockListedDateRepositorySupport stockListedDateRepositorySupport;

    @Transactional(readOnly = true)
    public List<StockListedDate> findAllByMarketAndDate(MarketType marketType, LocalDate date) {
        return stockListedDateRepository.findAllById_MarketTypeAndId_Date(marketType, date);
    }

    @Transactional(readOnly = true)
    public Set<String> findAllCodesByMarketAndDate(MarketType marketType, LocalDate date) {
        return stockListedDateRepository.findAllById_MarketTypeAndId_Date(marketType, date)
                .stream().map(r -> r.getId().getCode()).collect(Collectors.toSet());
    }

    @Transactional(readOnly = true)
    public boolean existsByMarketAndCodeAndDate(MarketType marketType, String code, LocalDate date) {
        return stockListedDateRepository.existsById_MarketTypeAndId_CodeAndId_Date(marketType, code, date);
    }

    @Transactional(readOnly = true)
    public boolean existsByMarketAndDate(MarketType marketType, LocalDate date) {
        return stockListedDateRepository.existsById_MarketTypeAndId_Date(marketType, date);
    }

    @Transactional(readOnly = true)
    public Optional<LocalDate> findLatestDateByMarket(MarketType marketType) {
        return stockListedDateRepository.findFirstById_MarketTypeOrderById_DateDesc(marketType)
                .map(r -> r.getId().getDate());
    }

    @Transactional(readOnly = true)
    public Set<LocalDate> findExistingDatesInRange(MarketType marketType, LocalDate from, LocalDate to) {
        return new HashSet<>(stockListedDateRepositorySupport.findDistinctDatesInRange(marketType, from, to));
    }
}
