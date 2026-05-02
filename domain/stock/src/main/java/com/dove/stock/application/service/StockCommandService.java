package com.dove.stock.application.service;

import com.dove.market.domain.enums.MarketType;
import com.dove.stock.domain.entity.Stock;
import com.dove.stock.domain.enums.TradingStatus;
import com.dove.stock.domain.repository.StockRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

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

    /** 최신 거래일의 주가·상장 데이터를 기준으로 전 종목 TradingStatus 조정. */
    @Transactional
    public void reconcileTradingStatuses(MarketType market, Set<String> listedCodes, Set<String> tradedCodes) {
        stockRepository.findAllById_MarketType(market).forEach(stock -> {
            TradingStatus next = resolveStatus(stock.getId().getCode(), listedCodes, tradedCodes);
            if (stock.getTradingStatus() != next) {
                stock.updateTradingStatus(next);
            }
        });
    }

    private static TradingStatus resolveStatus(String code, Set<String> listedCodes, Set<String> tradedCodes) {
        if (!listedCodes.contains(code)) return TradingStatus.DELISTED;
        return tradedCodes.contains(code) ? TradingStatus.ACTIVE : TradingStatus.SUSPENDED;
    }
}
