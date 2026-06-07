package com.dove.kis.infrastructure.adapter;

/**
 * KIS API가 실패 응답 코드를 반환했을 때 발생하는 예외.
 */
public class KisApiException extends RuntimeException {

    private final String code;

    public KisApiException(String code, String message) {
        super("[" + code + "] " + message);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
