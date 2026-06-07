package com.dove.apiquota;

/**
 * API 호출 한도의 종류 (일일 누적 / 초당 동시).
 */
public enum QuotaType {
    DAILY,
    PER_SECOND
}
