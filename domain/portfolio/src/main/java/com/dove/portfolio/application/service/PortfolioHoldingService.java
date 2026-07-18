package com.dove.portfolio.application.service;

import com.dove.portfolio.domain.entity.PortfolioHolding;
import com.dove.portfolio.domain.enums.PortfolioMarket;
import com.dove.portfolio.domain.repository.PortfolioAccountRepository;
import com.dove.portfolio.domain.repository.PortfolioHoldingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;

/**
 * 포트폴리오 종목 식별 정보 서비스 (조회 메서드만 readOnly).
 */
@Service
@RequiredArgsConstructor
public class PortfolioHoldingService {

    private final PortfolioHoldingRepository repository;
    private final PortfolioAccountRepository accountRepository;

    /**
     * 소유 회원의 종목 식별 정보를 조회한다.
     */
    @Transactional(readOnly = true)
    public List<PortfolioHolding> findByOwner(Long ownerMemberId) {
        return repository.findByOwnerMemberIdOrderByIdAsc(ownerMemberId);
    }

    /**
     * (계좌, 종목)의 시장·티커를 등록하거나 갱신한다.
     *
     * @throws NoSuchElementException 대상 계좌가 없거나 본인 소유가 아닐 때
     */
    @Transactional
    public PortfolioHolding attach(Long ownerMemberId, Long accountId, String symbol,
                                 PortfolioMarket market, String ticker, String actor) {
        requireOwnedAccount(ownerMemberId, accountId);
        return repository.findByOwnerMemberIdAndAccountIdAndSymbol(ownerMemberId, accountId, symbol)
                .map(h -> {
                    h.updateIdentity(market, ticker, actor);
                    return h;
                })
                .orElseGet(() -> repository.save(
                        PortfolioHolding.create(ownerMemberId, accountId, symbol, market, ticker, actor)));
    }

    /**
     * 보유 종목의 연 배당수익률(%)을 설정한다.
     *
     * @throws NoSuchElementException 해당 보유가 없을 때
     */
    @Transactional
    public PortfolioHolding setDividend(Long ownerMemberId, Long id, Double annualDividendPct, String actor) {
        PortfolioHolding h = repository.findByIdAndOwnerMemberId(id, ownerMemberId)
                .orElseThrow(() -> new NoSuchElementException("PORTFOLIO_HOLDING_NOT_FOUND"));
        h.updateDividend(annualDividendPct, actor);
        return h;
    }

    /**
     * 보유 종목의 배당 추적 대상 여부를 설정한다.
     *
     * @throws NoSuchElementException 해당 보유가 없을 때
     */
    @Transactional
    public PortfolioHolding setTracking(Long ownerMemberId, Long id, boolean tracked, String actor) {
        PortfolioHolding h = repository.findByIdAndOwnerMemberId(id, ownerMemberId)
                .orElseThrow(() -> new NoSuchElementException("PORTFOLIO_HOLDING_NOT_FOUND"));
        h.updateTracking(tracked, actor);
        return h;
    }

    /**
     * 종목 식별 정보를 삭제한다.
     *
     * @throws NoSuchElementException 해당 식별 정보가 없을 때
     */
    @Transactional
    public void delete(Long ownerMemberId, Long id) {
        PortfolioHolding h = repository.findByIdAndOwnerMemberId(id, ownerMemberId)
                .orElseThrow(() -> new NoSuchElementException("PORTFOLIO_HOLDING_NOT_FOUND"));
        repository.delete(h);
    }

    private void requireOwnedAccount(Long ownerMemberId, Long accountId) {
        accountRepository.findByIdAndOwnerMemberId(accountId, ownerMemberId)
                .orElseThrow(() -> new NoSuchElementException("PORTFOLIO_ACCOUNT_NOT_FOUND"));
    }
}
