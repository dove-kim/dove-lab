package com.dove.stock.application.service;

import com.dove.stock.domain.entity.StockListedDate;
import com.dove.stock.domain.repository.StockListedDateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** 종목 상장 이력 변경 전용 서비스. */
@Service
@RequiredArgsConstructor
public class StockListedDateCommandService {

    private final StockListedDateRepository stockListedDateRepository;

    @Transactional
    public List<StockListedDate> saveAll(List<StockListedDate> rows) {
        return stockListedDateRepository.saveAll(rows);
    }
}
