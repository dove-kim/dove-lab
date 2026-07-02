package com.dove.modelserving.application.service;

import com.dove.modelserving.domain.entity.StockModelScore;
import com.dove.modelserving.infrastructure.repository.StockModelScoreRepositorySupport;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 모델 채점 점수를 조회하는 서비스.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ModelScoreQueryService {

    private final StockModelScoreRepositorySupport scoreSupport;

    /**
     * 한 모델·종목의 점수를 거래일 오름차순으로 반환한다.
     */
    public List<StockModelScore> findByModelAndTicker(Long modelId, String ticker) {
        return scoreSupport.findByModelAndTicker(modelId, ticker);
    }

    /**
     * 한 모델·종목의 거래일 구간[from, to] 점수를 거래일 오름차순으로 반환한다. 경계가 null이면 무제한.
     */
    public List<StockModelScore> findByModelTickerAndDateRange(Long modelId, String ticker,
                                                               LocalDate from, LocalDate to) {
        return scoreSupport.findByModelTickerAndDateRange(modelId, ticker, from, to);
    }

    /**
     * 한 모델·거래일의 전 종목 점수를 ticker→점수 맵으로 반환한다.
     */
    public Map<String, Double> findScoresByModelAndDate(Long modelId, LocalDate date) {
        Map<String, Double> result = new HashMap<>();
        for (StockModelScore s : scoreSupport.findByModelAndDate(modelId, date)) {
            result.put(s.getId().getTicker(), s.getScore().doubleValue());
        }
        return result;
    }
}
