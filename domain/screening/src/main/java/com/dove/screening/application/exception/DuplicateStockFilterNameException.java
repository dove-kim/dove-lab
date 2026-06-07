package com.dove.screening.application.exception;

/**
 * 동일 소유자(시스템 또는 회원) 내에서 종목 필터 이름이 중복될 때 발생.
 */
public class DuplicateStockFilterNameException extends RuntimeException {
    public DuplicateStockFilterNameException(String message) {
        super(message);
    }
}
