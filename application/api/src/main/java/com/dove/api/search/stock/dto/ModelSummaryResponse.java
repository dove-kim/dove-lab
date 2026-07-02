package com.dove.api.search.stock.dto;

import com.dove.modelserving.domain.entity.MlModel;

/**
 * 활성 모델 요약(차트·필터에서 모델 선택용).
 *
 * @param id         모델 식별자
 * @param name       모델 이름
 * @param version    버전
 * @param outputType 출력 의미(PROBABILITY/REGRESSION)
 */
public record ModelSummaryResponse(Long id, String name, String version, String outputType) {

    /**
     * 모델 엔티티를 요약 응답으로 변환한다.
     */
    public static ModelSummaryResponse from(MlModel model) {
        return new ModelSummaryResponse(
                model.getId(), model.getName(), model.getVersion(), model.getOutputType().name());
    }
}
