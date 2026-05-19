package com.dove.screening.application.service;

import com.dove.screening.domain.entity.StockSet;
import com.dove.screening.domain.repository.StockSetRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StockSetQueryService {

    private final StockSetRepository stockSetRepository;

    public List<StockSet> findAllByMemberId(Long memberId) {
        return stockSetRepository.findAllByMemberIdOrderByCreatedAtDesc(memberId);
    }

    public Optional<StockSet> findByIdAndMemberId(Long id, Long memberId) {
        return stockSetRepository.findByIdAndMemberId(id, memberId);
    }
}
