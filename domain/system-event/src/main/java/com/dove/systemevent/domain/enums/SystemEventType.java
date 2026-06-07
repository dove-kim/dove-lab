package com.dove.systemevent.domain.enums;

/**
 * 시스템 이벤트 종류.
 */
public enum SystemEventType {
    /**
     * KRX API 호출 실패.
     */
    KRX_API_FAILURE,
    /**
     * KRX API 일일 한도 초과 (서버 응답 기반).
     */
    KRX_RATE_LIMIT_EXCEEDED,
    /**
     * KIS API 호출 실패 — 일일 수집 또는 백필 중단.
     */
    KIS_API_FAILURE
}
