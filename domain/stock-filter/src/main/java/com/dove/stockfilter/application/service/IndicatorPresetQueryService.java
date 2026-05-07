package com.dove.stockfilter.application.service;

import com.dove.stockfilter.domain.entity.IndicatorPreset;
import com.dove.stockfilter.domain.repository.IndicatorPresetRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class IndicatorPresetQueryService {

    private final IndicatorPresetRepository indicatorPresetRepository;

    public List<IndicatorPreset> findAllByMemberId(Long memberId) {
        return indicatorPresetRepository.findAllByMemberIdOrderByDisplayOrderAscCreatedAtAsc(memberId);
    }
}
