package com.dove.stockprice.application.service;

import com.dove.stockprice.domain.entity.StockDataChange;
import com.dove.stockprice.domain.repository.StockDataChangeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** 주가 변경 기록 쓰기 전용 서비스. */
@Service
@RequiredArgsConstructor
public class StockDataChangeCommandService {

    private final StockDataChangeRepository stockDataChangeRepository;

    @Transactional
    public void save(StockDataChange change) {
        stockDataChangeRepository.save(change);
    }

    @Transactional
    public void deleteAll(List<StockDataChange> changes) {
        stockDataChangeRepository.deleteChanges(changes);
    }
}
