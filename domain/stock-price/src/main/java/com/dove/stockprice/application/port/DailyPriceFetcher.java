package com.dove.stockprice.application.port;

import com.dove.market.domain.enums.MarketType;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/** 일별 종목 시세 조회 포트. 결과·실패 분류를 자기 어휘(Outcome, Reason)로 표현. */
public interface DailyPriceFetcher {
    Outcome fetchDailyMarketData(MarketType market, LocalDate targetDate);

    sealed interface Outcome {
        record Success(List<StockInfo> stocks) implements Outcome {}

        record Holiday() implements Outcome {}

        record RetryLater(Reason reason, Instant nextRetryAt, String detail) implements Outcome {}

        record PermanentFail(Reason reason, String detail) implements Outcome {}
    }

    /** 시세 조회 실패 분류. UNCERTAIN은 확정 전 빈 응답. */
    enum Reason {
        UNCERTAIN,
        TRANSIENT,
        AUTH_FAILED,
        PARSE_FAILED
    }
}
