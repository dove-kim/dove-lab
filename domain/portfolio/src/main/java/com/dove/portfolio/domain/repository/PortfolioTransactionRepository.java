package com.dove.portfolio.domain.repository;

import com.dove.portfolio.domain.entity.PortfolioTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * 포트폴리오 거래 영속성 저장소.
 */
public interface PortfolioTransactionRepository extends JpaRepository<PortfolioTransaction, Long> {

    /** 소유 회원의 거래 목록 (최신순). */
    List<PortfolioTransaction> findByOwnerMemberIdOrderByTradeDateDescIdDesc(Long ownerMemberId);

    /** 거래 단건 — 본인 소유 확인 포함. */
    Optional<PortfolioTransaction> findByIdAndOwnerMemberId(Long id, Long ownerMemberId);
}
