package com.dove.modelserving.application.exception;

/**
 * 모델 등록 시 meta.json이 스키마·피처·해시 검증을 통과하지 못할 때 발생.
 */
public class InvalidModelMetaException extends RuntimeException {

    /**
     * @param message 영문 대문자 스네이크케이스 에러 코드(예: UNKNOWN_FEATURE, FEATURE_HASH_MISMATCH)
     */
    public InvalidModelMetaException(String message) {
        super(message);
    }
}
