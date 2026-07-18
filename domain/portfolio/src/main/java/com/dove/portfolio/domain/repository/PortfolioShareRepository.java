package com.dove.portfolio.domain.repository;

import com.dove.portfolio.domain.entity.PortfolioShare;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * 계좌 공유 grant 저장소.
 */
public interface PortfolioShareRepository extends JpaRepository<PortfolioShare, Long> {

    List<PortfolioShare> findByOwnerMemberIdOrderByIdDesc(Long ownerMemberId);

    List<PortfolioShare> findByGranteeMemberIdOrderByIdDesc(Long granteeMemberId);

    Optional<PortfolioShare> findByAccountIdAndGranteeMemberId(Long accountId, Long granteeMemberId);

    Optional<PortfolioShare> findByIdAndOwnerMemberId(Long id, Long ownerMemberId);
}
