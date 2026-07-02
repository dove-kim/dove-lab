package com.dove.modelserving.infrastructure.scorer;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * 채점기 stdin으로 보내는 한 배치 입력.
 *
 * @param modelId   모델 ID(형식 검증·로깅용)
 * @param modelPath 채점기가 읽을 모델 아티팩트(.pkl) 경로
 * @param rows      채점할 행들
 */
public record PredictInput(
        @JsonProperty("model_id") Long modelId,
        @JsonProperty("model_path") String modelPath,
        @JsonProperty("rows") List<PredictRow> rows) {
}
