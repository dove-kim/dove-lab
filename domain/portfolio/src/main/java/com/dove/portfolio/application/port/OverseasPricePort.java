package com.dove.portfolio.application.port;

import com.dove.portfolio.domain.enums.PortfolioMarket;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * 외부 소스에서 해외 종목의 최신 종가(원통화)를 가져오는 포트.
 */
public interface OverseasPricePort {

    /**
     * 해외 종목의 최신 종가(원통화)를 조회한다. 실패하면 빈 값을 반환한다.
     *
     * @param market 해외 시장(국내 시장이면 빈 값)
     * @param ticker 시장 내 종목 코드
     */
    Optional<BigDecimal> fetchClose(PortfolioMarket market, String ticker);
}
