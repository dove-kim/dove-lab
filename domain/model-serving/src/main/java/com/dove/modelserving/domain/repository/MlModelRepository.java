package com.dove.modelserving.domain.repository;

import com.dove.modelserving.domain.entity.MlModel;
import com.dove.modelserving.domain.enums.ModelStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * 등록된 ML 모델 CRUD 저장소.
 */
public interface MlModelRepository extends JpaRepository<MlModel, Long> {

    /**
     * 지정 상태의 모델을 모두 반환한다.
     */
    List<MlModel> findByStatus(ModelStatus status);

    /**
     * 이름·버전이 일치하는 모델을 반환한다.
     */
    Optional<MlModel> findByNameAndVersion(String name, String version);
}
