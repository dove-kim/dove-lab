package com.dove.modelserving.application.exception;

/**
 * 채점 도중 모델의 채점 커서가 기대값과 달라졌음을 나타내는 예외(CAS 전진 실패).
 */
public class ScoreCursorRewoundException extends RuntimeException {

    public ScoreCursorRewoundException(Long modelId) {
        super("모델 채점 커서 변경 감지(rewound): modelId=" + modelId);
    }
}
