package com.dove.portfolio.application.service;

import com.dove.portfolio.domain.entity.PortfolioTransaction;
import com.dove.portfolio.domain.enums.TxType;
import com.dove.portfolio.domain.repository.PortfolioAccountRepository;
import com.dove.portfolio.domain.repository.PortfolioTransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * 포트폴리오 거래 서비스 (조회 메서드만 readOnly).
 */
@Service
@RequiredArgsConstructor
public class PortfolioTransactionService {

    private final PortfolioTransactionRepository repository;
    private final PortfolioAccountRepository accountRepository;

    /**
     * 소유 회원의 거래 목록을 최신순으로 조회한다.
     */
    @Transactional(readOnly = true)
    public List<PortfolioTransaction> findByOwner(Long ownerMemberId) {
        return repository.findByOwnerMemberIdOrderByTradeDateDescIdDesc(ownerMemberId);
    }

    /**
     * 거래를 생성한다.
     *
     * @throws NoSuchElementException 대상 계좌가 없거나 본인 소유가 아닐 때
     */
    @Transactional
    public PortfolioTransaction create(Long ownerMemberId, Long accountId, TxType type, LocalDate tradeDate,
                                     String symbol, String currency, BigDecimal quantity, BigDecimal price,
                                     BigDecimal amount, Long fee,
                                     String tag, String memo, String createdBy) {
        requireOwnedAccount(ownerMemberId, accountId);
        return repository.save(PortfolioTransaction.create(ownerMemberId, accountId, type, tradeDate, symbol, currency,
                quantity, price, amount, fee, tag, memo, createdBy));
    }

    /**
     * 거래를 수정한다.
     *
     * @throws NoSuchElementException 해당 거래가 없을 때
     */
    @Transactional
    public PortfolioTransaction update(Long ownerMemberId, Long id, TxType type, LocalDate tradeDate, String symbol,
                                     String currency, BigDecimal quantity, BigDecimal price,
                                     BigDecimal amount, Long fee, String tag, String memo, String updatedBy) {
        PortfolioTransaction tx = repository.findByIdAndOwnerMemberId(id, ownerMemberId)
                .orElseThrow(() -> new NoSuchElementException("PORTFOLIO_TRANSACTION_NOT_FOUND"));
        tx.update(type, tradeDate, symbol, currency, quantity, price, amount, fee, tag, memo, updatedBy);
        return tx;
    }

    /**
     * 거래를 삭제한다.
     *
     * @throws NoSuchElementException 해당 거래가 없을 때
     */
    @Transactional
    public void delete(Long ownerMemberId, Long id) {
        PortfolioTransaction tx = repository.findByIdAndOwnerMemberId(id, ownerMemberId)
                .orElseThrow(() -> new NoSuchElementException("PORTFOLIO_TRANSACTION_NOT_FOUND"));
        repository.delete(tx);
    }

    private void requireOwnedAccount(Long ownerMemberId, Long accountId) {
        accountRepository.findByIdAndOwnerMemberId(accountId, ownerMemberId)
                .orElseThrow(() -> new NoSuchElementException("PORTFOLIO_ACCOUNT_NOT_FOUND"));
    }
}
