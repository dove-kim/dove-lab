package com.dove.portfolio.application.port;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * 외부 환율 소스에서 원통화→원화 환율을 가져오는 포트.
 */
public interface FxRatePort {

    /**
     * 원통화 1단위당 원화 환율을 조회한다. 실패하면 빈 값을 반환한다(전일값 유지 판단은 호출자 몫).
     */
    Optional<FxQuote> fetchRateToKrw(String currency);

    /**
     * 지정 구간의 일자별 환율을 조회한다. 고시가 없는 날(휴일)은 결과에 포함되지 않으며, 실패하면 빈 목록을 반환한다.
     *
     * @param currency 원통화 코드
     * @param from     시작일(포함)
     * @param to       종료일(포함)
     */
    List<FxQuote> fetchSeriesToKrw(String currency, LocalDate from, LocalDate to);
}
