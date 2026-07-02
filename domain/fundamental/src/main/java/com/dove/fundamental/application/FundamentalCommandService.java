package com.dove.fundamental.application;

import com.dove.fundamental.domain.entity.StockFundamental;
import com.dove.fundamental.domain.entity.StockValuationDaily;
import com.dove.fundamental.domain.repository.StockFundamentalRepository;
import com.dove.fundamental.domain.repository.StockValuationDailyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 재무제표·밸류에이션 저장.
 */
@Service
@RequiredArgsConstructor
public class FundamentalCommandService {

    private final StockFundamentalRepository fundamentalRepository;
    private final StockValuationDailyRepository valuationRepository;

    /**
     * DART 재무제표 1건을 저장한다(공시 단위 upsert).
     */
    @Transactional
    public void saveStatement(StockFundamental fundamental) {
        fundamentalRepository.save(fundamental);
    }

    /**
     * 일별 밸류에이션 1건을 저장한다(멱등 upsert).
     */
    @Transactional
    public void saveValuation(StockValuationDaily valuation) {
        valuationRepository.save(valuation);
    }
}
