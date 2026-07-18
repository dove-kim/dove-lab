package com.dove.portfolio.domain.repository;

import com.dove.portfolio.domain.entity.PortfolioHolding;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * 포트폴리오 종목 식별 정보 영속성 저장소.
 */
public interface PortfolioHoldingRepository extends JpaRepository<PortfolioHolding, Long> {

    /** 소유 회원의 종목 식별 정보 목록. */
    List<PortfolioHolding> findByOwnerMemberIdOrderByIdAsc(Long ownerMemberId);

    /** (계좌, 종목)으로 식별 정보 단건 — upsert 판별용. */
    Optional<PortfolioHolding> findByOwnerMemberIdAndAccountIdAndSymbol(Long ownerMemberId, Long accountId, String symbol);

    /** 식별 정보 단건 — 본인 소유 확인 포함. */
    Optional<PortfolioHolding> findByIdAndOwnerMemberId(Long id, Long ownerMemberId);
}
