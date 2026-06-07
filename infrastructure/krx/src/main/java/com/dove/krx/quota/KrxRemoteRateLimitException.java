package com.dove.krx.quota;

/**
 * KRX 서버가 한도 초과 응답을 보냄.
 */
public class KrxRemoteRateLimitException extends RuntimeException {
    public KrxRemoteRateLimitException(String detail) {
        super(detail);
    }
}
