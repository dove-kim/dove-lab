package com.dove.portfolio.application.service;

import com.dove.portfolio.domain.entity.PortfolioRebalancePlan;
import com.dove.portfolio.domain.repository.PortfolioRebalancePlanRepository;
import com.dove.portfolio.domain.value.RebalancePlanEntry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;

/**
 * 리밸런싱 계획 서비스 — 이름 기준 upsert(조회 메서드만 readOnly).
 */
@Service
@RequiredArgsConstructor
public class PortfolioRebalancePlanService {

    private final PortfolioRebalancePlanRepository repository;

    /**
     * 소유 회원의 계획 목록을 이름순으로 조회한다.
     */
    @Transactional(readOnly = true)
    public List<PortfolioRebalancePlan> findByOwner(Long ownerMemberId) {
        return repository.findByOwnerMemberIdOrderByNameAsc(ownerMemberId);
    }

    /**
     * 계획을 저장한다 — 같은 이름이 있으면 목표 배분을 갱신(upsert).
     */
    @Transactional
    public PortfolioRebalancePlan save(Long ownerMemberId, String name, List<RebalancePlanEntry> entries, String actor) {
        return repository.findByOwnerMemberIdAndName(ownerMemberId, name)
                .map(p -> {
                    p.updateEntries(entries, actor);
                    return p;
                })
                .orElseGet(() -> repository.save(PortfolioRebalancePlan.create(ownerMemberId, name, entries, actor)));
    }

    /**
     * 계획을 삭제한다.
     *
     * @throws NoSuchElementException 해당 계획이 없을 때
     */
    @Transactional
    public void delete(Long ownerMemberId, Long id) {
        PortfolioRebalancePlan p = repository.findByIdAndOwnerMemberId(id, ownerMemberId)
                .orElseThrow(() -> new NoSuchElementException("PORTFOLIO_REBALANCE_PLAN_NOT_FOUND"));
        repository.delete(p);
    }
}
