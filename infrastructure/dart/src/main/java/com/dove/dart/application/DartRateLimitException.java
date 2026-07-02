package com.dove.dart.application;

/**
 * DART 일일 사용한도 초과(status 020) 시 발생한다.
 */
public class DartRateLimitException extends RuntimeException {

    public DartRateLimitException(String message) {
        super(message);
    }
}
