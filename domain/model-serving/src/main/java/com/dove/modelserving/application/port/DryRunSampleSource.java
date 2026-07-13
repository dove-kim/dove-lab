package com.dove.modelserving.application.port;

import com.dove.modelserving.domain.entity.MlModel;
import com.dove.modelserving.infrastructure.scorer.PredictRow;

import java.util.List;

/**
 * 활성화 드라이런에 쓸 진입존 표본 행을 공급하는 포트.
 */
public interface DryRunSampleSource {

    /**
     * 모델의 진입존을 만족하는 최근 표본 행을 최대 limit개까지 채점기 입력 행으로 모아 반환한다.
     * 표본이 없으면 빈 목록을 반환한다.
     */
    List<PredictRow> sample(MlModel model, int limit);
}
