package com.dove.modelserving.application.exception;

/**
 * 모델 활성화 드라이런이 표본 부족·피처 불일치·출력 범위 위반 등으로 실패할 때 발생.
 */
public class ModelActivationException extends RuntimeException {

    /** 드라이런 표본이 하나도 없을 때. */
    public static final String DRY_RUN_NO_SAMPLE = "DRY_RUN_NO_SAMPLE";

    /** meta 피처 이름이 표본 입력 피처 키와 대소문자까지 일치하지 않을 때. */
    public static final String DRY_RUN_FEATURE_MISMATCH = "DRY_RUN_FEATURE_MISMATCH";

    /** 표본 점수가 NaN·무한이거나 확률 범위를 벗어나거나 유효 점수가 하나도 없을 때. */
    public static final String DRY_RUN_INVALID_OUTPUT = "DRY_RUN_INVALID_OUTPUT";

    /** 드라이런 채점기 실행이 실패했을 때. */
    public static final String DRY_RUN_SCORING_FAILED = "DRY_RUN_SCORING_FAILED";

    /**
     * @param message 영문 대문자 스네이크케이스 에러 코드(예: DRY_RUN_FEATURE_MISMATCH)
     */
    public ModelActivationException(String message) {
        super(message);
    }
}
