package com.dove.fundamental.application;

import com.dove.fundamental.domain.entity.StockValuationDaily;
import com.dove.fundamental.domain.repository.StockValuationDailyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

/**
 * 일별 밸류에이션 스칼라 조회(스크리닝 정렬용).
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StockValuationQueryService {

    private final StockValuationDailyRepository valuationRepository;

    /**
     * 특정 거래일의 종목별 시가총액을 ticker→시총 맵으로 반환한다(시총 없는 종목은 제외).
     */
    public Map<String, Long> findMarketCapByDate(LocalDate date) {
        Map<String, Long> byTicker = new HashMap<>();
        for (StockValuationDaily v : valuationRepository.findByTradeDate(date)) {
            if (v.getMarketCap() != null) byTicker.put(v.getTicker(), v.getMarketCap());
        }
        return byTicker;
    }
}
