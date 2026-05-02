package com.dove.stock.application.service;

import com.dove.stock.domain.entity.StockListedDate;
import com.dove.stock.domain.repository.StockListedDateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class StockListedDateCommandService {

    private final StockListedDateRepository stockListedDateRepository;

    @Transactional
    public List<StockListedDate> saveAll(List<StockListedDate> rows) {
        return stockListedDateRepository.saveAll(rows);
    }
}
