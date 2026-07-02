package com.dove.modelserving.application.exception;

/**
 * 모델 채점 실행(채점기 호출·결과 해석)이 실패했음을 나타내는 예외.
 */
public class ModelScoringException extends RuntimeException {

    private final String errorCode;

    public ModelScoringException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public ModelScoringException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    /**
     * 채점기가 돌려준 에러 코드(영문 대문자 스네이크케이스).
     */
    public String errorCode() {
        return errorCode;
    }
}
