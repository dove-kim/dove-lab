package com.dove.modelserving.application.service;

import com.dove.modelserving.application.exception.ModelNotFoundException;
import com.dove.modelserving.domain.entity.MlModel;
import com.dove.modelserving.domain.repository.MlModelRepository;
import com.dove.modelserving.infrastructure.repository.StockModelScoreRepositorySupport;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

/**
 * 모델의 활성화·비활성화·커서 리셋·삭제 생명주기를 다루는 서비스.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class ModelLifecycleService {

    private final MlModelRepository modelRepository;
    private final StockModelScoreRepositorySupport scoreSupport;

    /**
     * 모델을 ACTIVE로 전환한다.
     *
     * @throws ModelNotFoundException 모델이 없을 때
     */
    public MlModel activate(Long modelId) {
        MlModel model = find(modelId);
        model.activate();
        return model;
    }

    /**
     * 모델을 INACTIVE로 전환한다.
     *
     * @throws ModelNotFoundException 모델이 없을 때
     */
    public MlModel deactivate(Long modelId) {
        MlModel model = find(modelId);
        model.deactivate();
        return model;
    }

    /**
     * 채점 커서를 지정 거래일로 되돌리고, 그 거래일 이후의 점수를 삭제한다. null이면 미시작으로 되돌리고 전 점수를 삭제한다.
     *
     * @throws ModelNotFoundException 모델이 없을 때
     */
    public MlModel resetScoreCursor(Long modelId, LocalDate toDate) {
        MlModel model = find(modelId);
        scoreSupport.deleteByModelAndDateRange(modelId, toDate == null ? null : toDate.plusDays(1), null);
        model.resetScoreCursor(toDate);
        return model;
    }

    /**
     * 모델과 그 모델의 모든 점수를 함께 삭제한다.
     *
     * @throws ModelNotFoundException 모델이 없을 때
     */
    public void delete(Long modelId) {
        MlModel model = find(modelId);
        scoreSupport.deleteByModel(modelId);
        modelRepository.delete(model);
    }

    private MlModel find(Long modelId) {
        return modelRepository.findById(modelId)
                .orElseThrow(() -> new ModelNotFoundException(modelId));
    }
}
