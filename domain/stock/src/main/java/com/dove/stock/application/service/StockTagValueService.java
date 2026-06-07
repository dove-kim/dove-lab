package com.dove.stock.application.service;

import com.dove.stock.domain.entity.StockTagValue;
import com.dove.stock.domain.repository.StockTagValueRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;

/**
 * 종목 분류 값 마스터 등록·표시명 변경·조회.
 */
@Service
@RequiredArgsConstructor
public class StockTagValueService {

    private final StockTagValueRepository repository;

    /**
     * 분류 값을 멱등 등록한다. 이미 있거나 빈 값이면 무시한다.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registerIfAbsent(String field, String value) {
        if (value == null || value.isBlank()) return;
        if (repository.existsByFieldAndValue(field, value)) return;
        try {
            repository.save(StockTagValue.of(field, value));
        } catch (DataIntegrityViolationException e) {
            // 동시 등록 충돌 — 이미 등록됨, 무시
        }
    }

    /**
     * 표시명을 변경한다.
     *
     * @throws NoSuchElementException 대상 분류 값이 없는 경우
     */
    @Transactional
    public void updateLabel(Long id, String label) {
        StockTagValue tag = repository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("STOCK_TAG_VALUE_NOT_FOUND"));
        tag.updateLabel(label);
    }

    /**
     * 전체 분류 값을 field·value 순으로 반환한다.
     */
    @Transactional(readOnly = true)
    public List<StockTagValue> findAll() {
        return repository.findAllByOrderByFieldAscValueAsc();
    }
}
