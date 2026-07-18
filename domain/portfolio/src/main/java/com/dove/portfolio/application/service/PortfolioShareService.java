package com.dove.portfolio.application.service;

import com.dove.portfolio.domain.entity.PortfolioShare;
import com.dove.portfolio.domain.enums.PortfolioSharePermission;
import com.dove.portfolio.domain.repository.PortfolioShareRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

/**
 * 계좌 공유 grant 서비스 (단순 위임 aggregate — 단일 서비스, 조회 메서드만 readOnly).
 */
@Service
@RequiredArgsConstructor
public class PortfolioShareService {

    private final PortfolioShareRepository repository;

    /**
     * 계좌를 회원에게 공유한다. 이미 공유돼 있으면 권한만 갱신한다(멱등).
     */
    @Transactional
    public PortfolioShare grant(Long ownerMemberId, Long accountId, Long granteeMemberId,
                                PortfolioSharePermission permission, String createdBy) {
        return repository.findByAccountIdAndGranteeMemberId(accountId, granteeMemberId)
                .map(existing -> {
                    existing.changePermission(permission);
                    return existing;
                })
                .orElseGet(() -> repository.save(
                        PortfolioShare.create(ownerMemberId, accountId, granteeMemberId, permission, createdBy)));
    }

    /**
     * 공유를 회수한다.
     *
     * @throws NoSuchElementException 소유자의 해당 공유가 없을 때
     */
    @Transactional
    public void revoke(Long ownerMemberId, Long shareId) {
        PortfolioShare share = repository.findByIdAndOwnerMemberId(shareId, ownerMemberId)
                .orElseThrow(() -> new NoSuchElementException("PORTFOLIO_SHARE_NOT_FOUND"));
        repository.delete(share);
    }

    /**
     * 소유 회원이 내보낸(공유한) grant 목록을 조회한다.
     */
    @Transactional(readOnly = true)
    public List<PortfolioShare> listByOwner(Long ownerMemberId) {
        return repository.findByOwnerMemberIdOrderByIdDesc(ownerMemberId);
    }

    /**
     * 회원이 받은(공유된) grant 목록을 조회한다.
     */
    @Transactional(readOnly = true)
    public List<PortfolioShare> listByGrantee(Long granteeMemberId) {
        return repository.findByGranteeMemberIdOrderByIdDesc(granteeMemberId);
    }

    /**
     * 회원이 특정 계좌에 대해 가진 공유 권한을 조회한다(없으면 empty).
     */
    @Transactional(readOnly = true)
    public Optional<PortfolioSharePermission> permissionFor(Long memberId, Long accountId) {
        return repository.findByAccountIdAndGranteeMemberId(accountId, memberId)
                .map(PortfolioShare::getPermission);
    }
}
