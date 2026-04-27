package com.dove.stock.application.port;

import com.dove.market.domain.enums.MarketType;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;

/**
 * 상장 종목 조회 포트. 결과·실패 분류를 자기 어휘(Outcome, Reason)로 표현.
 * - Success: (stockCode → stockName) 맵
 * - Holiday: 휴장일
 * - RetryLater: 일시적 장애 — 재시도 예약
 * - PermanentFail: 인증/파싱 실패 — 수동 개입
 */
public interface StockListingFetcher {
    Outcome fetch(MarketType market, LocalDate date);

    sealed interface Outcome {
        record Success(Map<String, String> stocks) implements Outcome {}

        record Holiday() implements Outcome {}

        record RetryLater(Reason reason, Instant nextRetryAt, String detail) implements Outcome {}

        record PermanentFail(Reason reason, String detail) implements Outcome {}
    }

    /** 상장 조회 실패 분류. */
    enum Reason {
        TRANSIENT,
        AUTH_FAILED,
        PARSE_FAILED
    }
}
