package com.dove.indicator.application.service;

import com.dove.indicator.domain.entity.TechnicalIndicator;
import com.dove.indicator.domain.repository.TechnicalIndicatorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** 기술적 지표 저장 전용 서비스. */
@Service
@RequiredArgsConstructor
public class TechnicalIndicatorCommandService {

    private final TechnicalIndicatorRepository technicalIndicatorRepository;

    @Transactional
    public void saveAll(List<TechnicalIndicator> indicators) {
        technicalIndicatorRepository.saveAll(indicators);
    }
}
