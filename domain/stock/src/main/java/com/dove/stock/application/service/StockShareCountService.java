package com.dove.stock.application.service;

import com.dove.stock.domain.entity.StockShareCount;
import com.dove.stock.domain.repository.StockShareCountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Optional;

/**
 * 상장주식수 변경이력 조회·저장. (단순 aggregate — 조회는 readOnly)
 */
@Service
@RequiredArgsConstructor
public class StockShareCountService {

    private final StockShareCountRepository repository;

    /**
     * 기준일 이하 중 가장 최근(as-of) 상장주식수를 반환한다.
     */
    @Transactional(readOnly = true)
    public Optional<StockShareCount> findAsOf(String ticker, LocalDate date) {
        return repository.findFirstByTickerAndEffectiveDateLessThanEqualOrderByEffectiveDateDesc(ticker, date);
    }

    /**
     * 상장주식수 변경이력 1건을 저장한다(멱등 upsert).
     */
    @Transactional
    public void save(StockShareCount shareCount) {
        repository.save(shareCount);
    }
}
