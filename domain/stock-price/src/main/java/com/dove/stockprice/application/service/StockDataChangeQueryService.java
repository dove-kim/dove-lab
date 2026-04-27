package com.dove.stockprice.application.service;

import com.dove.stockprice.domain.entity.StockDataChange;
import com.dove.stockprice.domain.repository.StockDataChangeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/** 주가 변경 기록 조회 전용 서비스. */
@Service
@RequiredArgsConstructor
public class StockDataChangeQueryService {

    private final StockDataChangeRepository stockDataChangeRepository;

    @Transactional(readOnly = true)
    public List<StockDataChange> findChangesOlderThan(LocalDateTime threshold) {
        return stockDataChangeRepository.findAllByCreatedAtBefore(threshold);
    }
}
