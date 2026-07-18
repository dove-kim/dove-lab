package com.dove.portfolio.domain.repository;

import com.dove.portfolio.domain.entity.PortfolioAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * 포트폴리오 계좌 영속성 저장소.
 */
public interface PortfolioAccountRepository extends JpaRepository<PortfolioAccount, Long> {

    /** 소유 회원의 계좌 목록. */
    List<PortfolioAccount> findByOwnerMemberIdOrderByIdAsc(Long ownerMemberId);

    /** 계좌 단건 — 본인 소유 확인 포함. */
    Optional<PortfolioAccount> findByIdAndOwnerMemberId(Long id, Long ownerMemberId);
}
