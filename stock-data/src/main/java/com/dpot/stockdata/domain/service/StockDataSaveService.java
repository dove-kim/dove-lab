package com.dpot.stockdata.domain.service;

import com.dpot.stockdata.domain.enums.StockMarketType;
import com.dpot.stockdata.repository.StockDataRepository;
import com.dpot.stockdata.repository.StockDateRepository;
import com.dpot.stockdata.repository.StockRepository;
import com.dpot.stockdata.repository.entity.Stock;
import com.dpot.stockdata.repository.entity.StockData;
import com.dpot.stockdata.repository.entity.StockDate;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class StockDataSaveService {
    private final StockRepository stockRepository;
    private final StockDateRepository stockDateRepository;
    private final StockDataRepository stockDataRepository;

    @Transactional
    public void update(
            LocalDate baseDate, StockMarketType stockMarketType,
            String stockCode, String stockName,
            Long volume,
            BigDecimal openingPrice, BigDecimal closingPrice,
            BigDecimal lowPrice, BigDecimal highPrice
    ) {
        // 주식 저장
        stockRepository
                .save(new Stock(stockMarketType, stockCode, stockName));

        // 주식 날짜 저장
        stockDateRepository
                .save(new StockDate(baseDate));

        // 주식 데이터 저장
        stockDataRepository
                .save(new StockData(
                        stockCode, baseDate, volume,
                        openingPrice, closingPrice, lowPrice, highPrice
                ));
    }
}
