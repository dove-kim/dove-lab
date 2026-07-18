package com.dove.portfolio.domain.repository;

import com.dove.portfolio.domain.entity.PortfolioFxConversion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * 환전 저장소.
 */
public interface PortfolioFxConversionRepository extends JpaRepository<PortfolioFxConversion, Long> {

    List<PortfolioFxConversion> findByOwnerMemberIdOrderByConvDateDescIdDesc(Long ownerMemberId);

    Optional<PortfolioFxConversion> findByIdAndOwnerMemberId(Long id, Long ownerMemberId);
}
