package com.dove.portfolio.domain.repository;

import com.dove.portfolio.domain.entity.PortfolioFxRate;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 원통화별 최신 환율 영속성 저장소.
 */
public interface PortfolioFxRateRepository extends JpaRepository<PortfolioFxRate, String> {
}
