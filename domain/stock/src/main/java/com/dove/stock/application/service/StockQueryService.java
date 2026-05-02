package com.dove.stock.application.service;

import com.dove.market.domain.enums.MarketType;
import com.dove.stock.domain.entity.Stock;
import com.dove.stock.domain.enums.TradingStatus;
import com.dove.stock.domain.repository.StockRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

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
    public List<Stock> findAllByStatus(TradingStatus status) {
        return stockRepository.findAllByTradingStatus(status);
    }

    @Transactional(readOnly = true)
    public List<Stock> findAllByStatusAndMarket(TradingStatus status, MarketType marketType) {
        return stockRepository.findAllByTradingStatusAndId_MarketType(status, marketType);
    }
}
