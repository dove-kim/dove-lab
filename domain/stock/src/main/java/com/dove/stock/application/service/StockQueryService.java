package com.dove.stock.application.service;

import com.dove.market.domain.enums.MarketType;
import com.dove.stock.domain.entity.Stock;
import com.dove.stock.domain.repository.StockRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StockQueryService {

    private final StockRepository stockRepository;

    @Transactional(readOnly = true)
    public Optional<Stock> findByMarketAndCode(MarketType marketType, String code) {
        return stockRepository.findByMarketTypeAndCode(marketType, code);
    }

    @Transactional(readOnly = true)
    public List<Stock> findAllByMarket(MarketType marketType) {
        return stockRepository.findAllById_MarketType(marketType);
    }

    @Transactional(readOnly = true)
    public List<Stock> findAll() {
        return stockRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Map<String, Stock> findByCodesAsMap(Collection<String> codes) {
        if (codes.isEmpty()) return Map.of();
        return stockRepository.findAllByCodeIn(codes).stream()
                .collect(Collectors.toMap(s -> s.getId().getCode(), s -> s));
    }
}
