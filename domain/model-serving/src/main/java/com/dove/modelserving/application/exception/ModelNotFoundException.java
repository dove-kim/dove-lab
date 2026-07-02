package com.dove.modelserving.application.exception;

/**
 * 지정한 ID의 모델을 찾을 수 없을 때 발생.
 */
public class ModelNotFoundException extends RuntimeException {

    /**
     * @param modelId 찾지 못한 모델 ID
     */
    public ModelNotFoundException(Long modelId) {
        super("MODEL_NOT_FOUND: " + modelId);
    }
}
