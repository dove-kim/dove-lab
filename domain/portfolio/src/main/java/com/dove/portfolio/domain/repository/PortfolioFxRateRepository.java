package com.dove.portfolio.domain.repository;

import com.dove.portfolio.domain.entity.PortfolioFxRate;
import com.dove.portfolio.domain.entity.PortfolioFxRateId;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * 원통화별 일자 환율 이력 영속성 저장소.
 */
public interface PortfolioFxRateRepository extends JpaRepository<PortfolioFxRate, PortfolioFxRateId> {

    /**
     * 해당 통화의 가장 최근 고시 환율을 조회한다.
     */
    Optional<PortfolioFxRate> findTopByIdCurrencyOrderByIdRateDateDesc(String currency);

    /**
     * 해당 통화의 지정일 이하 가장 최근 고시 환율을 조회한다.
     */
    Optional<PortfolioFxRate> findTopByIdCurrencyAndIdRateDateLessThanEqualOrderByIdRateDateDesc(
            String currency, LocalDate rateDate);

    /**
     * 해당 통화들의 전 이력을 조회한다.
     */
    List<PortfolioFxRate> findByIdCurrencyIn(Collection<String> currencies);

    /**
     * 해당 통화들의 최근 고시 환율을 고시일 내림차순으로 조회한다.
     */
    List<PortfolioFxRate> findByIdCurrencyInOrderByIdRateDateDesc(Collection<String> currencies, Pageable pageable);
}
