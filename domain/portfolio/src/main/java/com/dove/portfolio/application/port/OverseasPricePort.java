package com.dove.portfolio.application.port;

import com.dove.portfolio.domain.enums.PortfolioMarket;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * 외부 소스에서 해외 종목의 종가(원통화)를 가져오는 포트.
 */
public interface OverseasPricePort {

    /**
     * 해외 종목의 최신 종가(원통화)를 조회한다. 실패하면 빈 값을 반환한다.
     *
     * @param market 해외 시장(국내 시장이면 빈 값)
     * @param ticker 시장 내 종목 코드
     */
    Optional<BigDecimal> fetchClose(PortfolioMarket market, String ticker);

    /**
     * 해외 종목의 일자별 종가를 지정일 이후로 조회한다. 휴장일은 결과에 없으며, 실패하면 빈 목록을 반환한다.
     *
     * @param market 해외 시장(국내 시장이면 빈 목록)
     * @param ticker 시장 내 종목 코드
     * @param from   받아올 가장 이른 날짜(포함)
     */
    List<OverseasClose> fetchDailyCloses(PortfolioMarket market, String ticker, LocalDate from);
}
