package com.dove.dart.application;

/**
 * DART 고유번호 매핑 다운로드·파싱 실패 시 발생한다.
 */
public class DartCorpCodeException extends RuntimeException {

    public DartCorpCodeException(String message, Throwable cause) {
        super(message, cause);
    }
}
