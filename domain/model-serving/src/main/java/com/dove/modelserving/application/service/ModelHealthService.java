package com.dove.modelserving.application.service;

import com.dove.modelserving.domain.entity.MlModel;
import com.dove.modelserving.domain.repository.MlModelRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * 모델별 마지막 채점 성공·실패를 채점 트랜잭션과 독립적으로 기록하는 서비스.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ModelHealthService {

    private final MlModelRepository modelRepository;

    /**
     * 마지막 채점 성공을 기록한다. 모델이 없으면 무시한다.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordSuccess(Long modelId) {
        find(modelId).ifPresent(MlModel::recordScoreSuccess);
    }

    /**
     * 마지막 채점 실패 사유를 기록한다. 모델이 없으면 무시한다.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordFailure(Long modelId, String error) {
        find(modelId).ifPresent(model -> model.recordScoreFailure(error));
    }

    private Optional<MlModel> find(Long modelId) {
        Optional<MlModel> model = modelRepository.findById(modelId);
        if (model.isEmpty()) {
            log.warn("[model {}] 헬스 기록 대상 모델이 없어 무시", modelId);
        }
        return model;
    }
}
