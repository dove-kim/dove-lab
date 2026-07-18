package com.dove.portfolio.application.service;

import com.dove.portfolio.domain.entity.PortfolioFxConversion;
import com.dove.portfolio.domain.repository.PortfolioAccountRepository;
import com.dove.portfolio.domain.repository.PortfolioFxConversionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * 환전 서비스 (조회 메서드만 readOnly).
 */
@Service
@RequiredArgsConstructor
public class PortfolioFxConversionService {

    private final PortfolioFxConversionRepository repository;
    private final PortfolioAccountRepository accountRepository;

    /**
     * 소유 회원의 환전 목록을 최신순으로 조회한다.
     */
    @Transactional(readOnly = true)
    public List<PortfolioFxConversion> findByOwner(Long ownerMemberId) {
        return repository.findByOwnerMemberIdOrderByConvDateDescIdDesc(ownerMemberId);
    }

    /**
     * 환전을 생성한다.
     *
     * @throws NoSuchElementException 대상 계좌가 없거나 본인 소유가 아닐 때
     */
    @Transactional
    public PortfolioFxConversion create(Long ownerMemberId, Long accountId, LocalDate convDate,
                                        String fromCurrency, BigDecimal fromAmount,
                                        String toCurrency, BigDecimal toAmount, Long fee,
                                        String memo, String createdBy) {
        requireOwnedAccount(ownerMemberId, accountId);
        return repository.save(PortfolioFxConversion.create(ownerMemberId, accountId, convDate,
                fromCurrency, fromAmount, toCurrency, toAmount, fee, memo, createdBy));
    }

    /**
     * 환전을 수정한다.
     *
     * @throws NoSuchElementException 해당 환전이 없을 때
     */
    @Transactional
    public PortfolioFxConversion update(Long ownerMemberId, Long id, LocalDate convDate,
                                        String fromCurrency, BigDecimal fromAmount,
                                        String toCurrency, BigDecimal toAmount, Long fee,
                                        String memo, String updatedBy) {
        PortfolioFxConversion c = repository.findByIdAndOwnerMemberId(id, ownerMemberId)
                .orElseThrow(() -> new NoSuchElementException("PORTFOLIO_FX_CONVERSION_NOT_FOUND"));
        c.update(convDate, fromCurrency, fromAmount, toCurrency, toAmount, fee, memo, updatedBy);
        return c;
    }

    /**
     * 환전을 삭제한다.
     *
     * @throws NoSuchElementException 해당 환전이 없을 때
     */
    @Transactional
    public void delete(Long ownerMemberId, Long id) {
        PortfolioFxConversion c = repository.findByIdAndOwnerMemberId(id, ownerMemberId)
                .orElseThrow(() -> new NoSuchElementException("PORTFOLIO_FX_CONVERSION_NOT_FOUND"));
        repository.delete(c);
    }

    private void requireOwnedAccount(Long ownerMemberId, Long accountId) {
        accountRepository.findByIdAndOwnerMemberId(accountId, ownerMemberId)
                .orElseThrow(() -> new NoSuchElementException("PORTFOLIO_ACCOUNT_NOT_FOUND"));
    }
}
