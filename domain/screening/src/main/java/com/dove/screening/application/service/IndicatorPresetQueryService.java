package com.dove.screening.application.service;

import com.dove.screening.domain.entity.IndicatorPreset;
import com.dove.screening.domain.repository.IndicatorPresetRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 지표 프리셋 조회 서비스.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class IndicatorPresetQueryService {

    private final IndicatorPresetRepository indicatorPresetRepository;

    /**
     * 회원의 지표 프리셋 목록을 노출 순서대로 조회한다.
     */
    public List<IndicatorPreset> findAllByMemberId(Long memberId) {
        return indicatorPresetRepository.findAllByMemberIdOrderByDisplayOrderAscCreatedAtAsc(memberId);
    }
}
