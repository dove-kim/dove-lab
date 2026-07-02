package com.dove.modelserving.application.port;

import com.dove.modelserving.infrastructure.scorer.PredictInput;
import com.dove.modelserving.infrastructure.scorer.ScoredRow;

import java.util.List;

/**
 * 모델 아티팩트로 한 배치의 피처 행을 채점하는 채점기 포트.
 */
public interface ModelScorer {

    /**
     * 입력 배치를 채점해 행별 점수를 반환한다.
     *
     * @throws com.dove.modelserving.application.exception.ModelScoringException 채점기 실행·해석 실패 시
     */
    List<ScoredRow> score(PredictInput input);
}
