package com.dove.kis.quota;

/**
 * KIS 호출 슬롯 대기가 중단됐을 때 발생하는 예외.
 */
public class KisRateLimitException extends RuntimeException {
    public KisRateLimitException(String message) {
        super(message);
    }
}
