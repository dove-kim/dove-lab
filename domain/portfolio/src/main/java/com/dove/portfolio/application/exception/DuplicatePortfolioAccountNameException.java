package com.dove.portfolio.application.exception;

/**
 * 같은 소유자 내에 이미 존재하는 계좌명일 때 발생한다.
 */
public class DuplicatePortfolioAccountNameException extends RuntimeException {
    public DuplicatePortfolioAccountNameException(String message) {
        super(message);
    }
}
