package com.dove.portfolio.domain.repository;

import com.dove.portfolio.domain.entity.PortfolioQuote;
import com.dove.portfolio.domain.entity.PortfolioQuoteId;
import com.dove.portfolio.domain.enums.PortfolioMarket;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;

/**
 * 해외 종목 일자별 종가 이력 영속성 저장소.
 */
public interface PortfolioQuoteRepository extends JpaRepository<PortfolioQuote, PortfolioQuoteId> {

    /** 해당 종목의 가장 최근 종가 — 현재 평가용. */
    Optional<PortfolioQuote> findTopByIdMarketAndIdTickerOrderByIdAsOfDesc(PortfolioMarket market, String ticker);

    /** 해당 종목의 지정일 이하 가장 최근 종가 — 과거 시점 평가용(휴장일 흡수). */
    Optional<PortfolioQuote> findTopByIdMarketAndIdTickerAndIdAsOfLessThanEqualOrderByIdAsOfDesc(
            PortfolioMarket market, String ticker, LocalDate asOf);
}
