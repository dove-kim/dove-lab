package com.dove.stockprice.application.service;

import com.dove.stockprice.domain.entity.DailyStockPrice;
import com.dove.stockprice.domain.repository.DailyStockPriceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DailyStockPriceCommandService {

    private final DailyStockPriceRepository dailyStockPriceRepository;

    @Transactional
    public DailyStockPrice save(DailyStockPrice dailyStockPrice) {
        return dailyStockPriceRepository.save(dailyStockPrice);
    }
}
