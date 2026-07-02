package com.dove.modelserving.application.exception;

/**
 * 모델 활성화 드라이런이 표본 부족·피처 불일치·출력 범위 위반 등으로 실패할 때 발생.
 */
public class ModelActivationException extends RuntimeException {

    /**
     * @param message 영문 대문자 스네이크케이스 에러 코드(예: DRY_RUN_FEATURE_MISMATCH)
     */
    public ModelActivationException(String message) {
        super(message);
    }
}
