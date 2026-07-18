package com.dove.portfolio.domain.repository;

import com.dove.portfolio.domain.entity.PortfolioRebalancePlan;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * 리밸런싱 계획 저장소.
 */
public interface PortfolioRebalancePlanRepository extends JpaRepository<PortfolioRebalancePlan, Long> {

    List<PortfolioRebalancePlan> findByOwnerMemberIdOrderByNameAsc(Long ownerMemberId);

    Optional<PortfolioRebalancePlan> findByIdAndOwnerMemberId(Long id, Long ownerMemberId);

    Optional<PortfolioRebalancePlan> findByOwnerMemberIdAndName(Long ownerMemberId, String name);
}
