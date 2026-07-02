package com.dove.modelserving.application.service;

/**
 * 한 모델 채점 결과. errorCode가 null이면 성공.
 *
 * @param modelId   모델 식별자
 * @param modelName 모델 이름
 * @param errorCode 실패 에러 코드(영문 대문자 스네이크케이스). 성공 시 null
 * @param message   실패 사유 메시지. 성공 시 null
 */
public record ModelScoringOutcome(Long modelId, String modelName, String errorCode, String message) {

    /**
     * 성공 결과를 만든다.
     */
    public static ModelScoringOutcome ok(Long modelId, String modelName) {
        return new ModelScoringOutcome(modelId, modelName, null, null);
    }

    /**
     * 실패 결과를 만든다.
     */
    public static ModelScoringOutcome failure(Long modelId, String modelName, String errorCode, String message) {
        return new ModelScoringOutcome(modelId, modelName, errorCode, message);
    }
}
