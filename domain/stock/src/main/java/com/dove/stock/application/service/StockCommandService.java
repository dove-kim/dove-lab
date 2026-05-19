package com.dove.stock.application.service;

import com.dove.stock.domain.entity.Stock;
import com.dove.stock.domain.repository.StockRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class StockCommandService {

    private final StockRepository stockRepository;

    @Transactional
    public Stock save(Stock stock) {
        return stockRepository.save(stock);
    }

    @Transactional
    public List<Stock> saveAll(List<Stock> stocks) {
        return stockRepository.saveAll(stocks);
    }
}
