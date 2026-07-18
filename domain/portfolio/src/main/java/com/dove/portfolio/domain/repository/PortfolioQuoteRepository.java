package com.dove.portfolio.domain.repository;

import com.dove.portfolio.domain.entity.PortfolioQuote;
import com.dove.portfolio.domain.enums.PortfolioMarket;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * 해외 종목 최신 종가 영속성 저장소.
 */
public interface PortfolioQuoteRepository extends JpaRepository<PortfolioQuote, Long> {

    /** (시장, 티커)로 종가 단건 — upsert 판별용. */
    Optional<PortfolioQuote> findByMarketAndTicker(PortfolioMarket market, String ticker);
}
