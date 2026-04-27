package com.dove.krxcalllog.application.service;

import com.dove.krxcalllog.domain.entity.KrxDailyData;
import com.dove.krxcalllog.domain.repository.KrxDailyDataRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** REQUIRES_NEW로 분리 저장되어 호출자 롤백에 영향받지 않는다. */
@Component
@RequiredArgsConstructor
public class KrxDailyDataAuditor {

    private final KrxDailyDataRepository krxDailyDataRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordFailure(KrxDailyData data) {
        krxDailyDataRepository.save(data);
    }
}
