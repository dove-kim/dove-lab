package com.dove.modelserving.application.service;

import com.dove.modelserving.application.exception.ModelNotFoundException;
import com.dove.modelserving.domain.entity.MlModel;
import com.dove.modelserving.domain.enums.ModelStatus;
import com.dove.modelserving.domain.repository.MlModelRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 등록된 모델을 조회하는 서비스.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ModelQueryService {

    private final MlModelRepository modelRepository;

    /**
     * 등록된 모든 모델을 반환한다.
     */
    public List<MlModel> findAll() {
        return modelRepository.findAll();
    }

    /**
     * 지정 상태의 모델을 모두 반환한다.
     */
    public List<MlModel> findByStatus(ModelStatus status) {
        return modelRepository.findByStatus(status);
    }

    /**
     * ID로 모델을 조회한다.
     *
     * @throws ModelNotFoundException 모델이 없을 때
     */
    public MlModel findById(Long modelId) {
        return modelRepository.findById(modelId)
                .orElseThrow(() -> new ModelNotFoundException(modelId));
    }
}
