package com.dove.portfolio.application.service;

import com.dove.portfolio.application.exception.DuplicatePortfolioAccountNameException;
import com.dove.portfolio.domain.entity.PortfolioAccount;
import com.dove.portfolio.domain.repository.PortfolioAccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;

/**
 * 포트폴리오 계좌 서비스 (단순 위임 aggregate — 단일 서비스, 조회 메서드만 readOnly).
 */
@Service
@RequiredArgsConstructor
public class PortfolioAccountService {

    private final PortfolioAccountRepository repository;

    /**
     * 소유 회원의 계좌 목록을 조회한다.
     */
    @Transactional(readOnly = true)
    public List<PortfolioAccount> findByOwner(Long ownerMemberId) {
        return repository.findByOwnerMemberIdOrderByIdAsc(ownerMemberId);
    }

    /**
     * 소유 회원의 계좌 단건을 조회한다.
     *
     * @throws NoSuchElementException 해당 계좌가 없을 때
     */
    @Transactional(readOnly = true)
    public PortfolioAccount getOwned(Long ownerMemberId, Long id) {
        return repository.findByIdAndOwnerMemberId(id, ownerMemberId)
                .orElseThrow(() -> new NoSuchElementException("PORTFOLIO_ACCOUNT_NOT_FOUND"));
    }

    /**
     * 소유자 무관 계좌 단건을 조회한다(공유 접근제어용).
     *
     * @throws NoSuchElementException 해당 계좌가 없을 때
     */
    @Transactional(readOnly = true)
    public PortfolioAccount getById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("PORTFOLIO_ACCOUNT_NOT_FOUND"));
    }

    /**
     * 계좌를 생성한다.
     *
     * @throws DuplicatePortfolioAccountNameException 같은 소유자에 같은 이름이 이미 있을 때
     */
    @Transactional
    public PortfolioAccount create(Long ownerMemberId, String name, String brokerName, String description, String createdBy) {
        try {
            return repository.saveAndFlush(PortfolioAccount.create(ownerMemberId, name, brokerName, description, createdBy));
        } catch (DataIntegrityViolationException e) {
            throw new DuplicatePortfolioAccountNameException("DUPLICATE_PORTFOLIO_ACCOUNT_NAME");
        }
    }

    /**
     * 계좌를 수정한다.
     *
     * @throws NoSuchElementException 해당 계좌가 없을 때
     * @throws DuplicatePortfolioAccountNameException 변경하려는 이름이 다른 계좌와 중복될 때
     */
    @Transactional
    public PortfolioAccount update(Long ownerMemberId, Long id, String name, String brokerName, String description, String updatedBy) {
        PortfolioAccount account = repository.findByIdAndOwnerMemberId(id, ownerMemberId)
                .orElseThrow(() -> new NoSuchElementException("PORTFOLIO_ACCOUNT_NOT_FOUND"));
        account.update(name, brokerName, description, updatedBy);
        try {
            repository.flush();
        } catch (DataIntegrityViolationException e) {
            throw new DuplicatePortfolioAccountNameException("DUPLICATE_PORTFOLIO_ACCOUNT_NAME");
        }
        return account;
    }

    /**
     * 계좌를 삭제한다.
     *
     * @throws NoSuchElementException 해당 계좌가 없을 때
     */
    @Transactional
    public void delete(Long ownerMemberId, Long id) {
        PortfolioAccount account = repository.findByIdAndOwnerMemberId(id, ownerMemberId)
                .orElseThrow(() -> new NoSuchElementException("PORTFOLIO_ACCOUNT_NOT_FOUND"));
        repository.delete(account);
    }
}
