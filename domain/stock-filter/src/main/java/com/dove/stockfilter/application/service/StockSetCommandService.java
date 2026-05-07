package com.dove.stockfilter.application.service;

import com.dove.stockfilter.domain.entity.StockSet;
import com.dove.stockfilter.domain.repository.StockSetRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
@Transactional
public class StockSetCommandService {

    private final StockSetRepository stockSetRepository;

    public StockSet create(Long memberId, String name, List<String> codes) {
        return stockSetRepository.save(StockSet.create(memberId, name, codes));
    }

    public StockSet update(Long memberId, Long id, String name, List<String> codes) {
        StockSet stockSet = stockSetRepository.findByIdAndMemberId(id, memberId)
                .orElseThrow(() -> new NoSuchElementException("STOCK_SET_NOT_FOUND"));
        stockSet.update(name, codes);
        return stockSet;
    }

    public void delete(Long memberId, Long id) {
        StockSet stockSet = stockSetRepository.findByIdAndMemberId(id, memberId)
                .orElseThrow(() -> new NoSuchElementException("STOCK_SET_NOT_FOUND"));
        stockSetRepository.delete(stockSet);
    }
}
