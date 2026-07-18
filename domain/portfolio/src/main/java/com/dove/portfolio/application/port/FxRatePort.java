package com.dove.portfolio.application.port;

import java.util.Optional;

/**
 * 외부 환율 소스에서 원통화→원화 환율을 가져오는 포트.
 */
public interface FxRatePort {

    /**
     * 원통화 1단위당 원화 환율을 조회한다. 실패하면 빈 값을 반환한다(전일값 유지 판단은 호출자 몫).
     */
    Optional<FxQuote> fetchRateToKrw(String currency);
}
