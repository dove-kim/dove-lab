package com.dove.stock.application.service;

import com.dove.market.domain.enums.MarketType;
import com.dove.stock.domain.entity.StockListedDate;
import com.dove.stock.domain.repository.StockListedDateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/** 종목 상장 이력 조회 전용 서비스. */
@Service
@RequiredArgsConstructor
public class StockListedDateQueryService {

    private final StockListedDateRepository stockListedDateRepository;

    @Transactional(readOnly = true)
    public List<StockListedDate> findAllByMarketAndDate(MarketType marketType, LocalDate date) {
        return stockListedDateRepository.findAllById_MarketTypeAndId_Date(marketType, date);
    }
}
