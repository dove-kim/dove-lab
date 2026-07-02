package com.dove.modelserving.application.service;

import com.dove.modelserving.infrastructure.repository.StockModelScoreRepositorySupport;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

/**
 * 모델 채점 점수를 삭제하는 서비스.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class ModelScoreCommandService {

    private final StockModelScoreRepositorySupport scoreSupport;

    /**
     * 한 모델의 모든 점수를 삭제하고 삭제된 행 수를 반환한다.
     */
    public long deleteAll(Long modelId) {
        return scoreSupport.deleteByModel(modelId);
    }

    /**
     * 한 모델에서 거래일 구간[from, to]의 점수를 삭제하고 삭제된 행 수를 반환한다. 경계가 null이면 무제한.
     */
    public long deleteByDateRange(Long modelId, LocalDate from, LocalDate to) {
        return scoreSupport.deleteByModelAndDateRange(modelId, from, to);
    }

    /**
     * 한 모델·종목의 점수를 삭제하고 삭제된 행 수를 반환한다.
     */
    public long deleteByTicker(Long modelId, String ticker) {
        return scoreSupport.deleteByModelAndTicker(modelId, ticker);
    }
}
