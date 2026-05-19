package com.dove.stock.application.service;

import com.dove.stock.domain.entity.DailyStockPrice;
import com.dove.stock.domain.repository.DailyStockPriceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DailyStockPriceCommandService {

    private final DailyStockPriceRepository dailyStockPriceRepository;

    @Transactional
    public DailyStockPrice save(DailyStockPrice dailyStockPrice) {
        return dailyStockPriceRepository.save(dailyStockPrice);
    }

    @Transactional
    public List<DailyStockPrice> saveAll(List<DailyStockPrice> prices) {
        return dailyStockPriceRepository.saveAll(prices);
    }
}
