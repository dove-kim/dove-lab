package com.dove.krx.quota;

/**
 * KRX 서버가 403 Access Denied로 접근을 차단함.
 * 과도한 요청으로 인한 일시 IP 차단 시 발생한다. 빈 응답과 구분해 호출자에 전파한다.
 */
public class KrxAccessBlockedException extends RuntimeException {
    public KrxAccessBlockedException(String detail) {
        super(detail);
    }
}
