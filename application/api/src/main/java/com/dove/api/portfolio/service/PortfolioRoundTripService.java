package com.dove.api.portfolio.service;

import com.dove.api.portfolio.dto.PortfolioRoundTripResponse;
import com.dove.portfolio.application.service.PortfolioAccountService;
import com.dove.portfolio.application.service.PortfolioFxRateService;
import com.dove.portfolio.application.service.PortfolioRoundTrip;
import com.dove.portfolio.application.service.PortfolioRoundTripCalculator;
import com.dove.portfolio.application.service.PortfolioTransactionService;
import com.dove.portfolio.domain.entity.PortfolioAccount;
import com.dove.portfolio.domain.entity.PortfolioTransaction;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 라운드트립 조합 — 거래 fold(청산 사이클)에 계좌명과 현재 환율(원화 환산)을 얹는다.
 */
@Service
@RequiredArgsConstructor
public class PortfolioRoundTripService {

    private final PortfolioTransactionService transactionService;
    private final PortfolioRoundTripCalculator roundTripCalculator;
    private final PortfolioAccountService accountService;
    private final PortfolioFxRateService fxRateService;

    /**
     * 소유 회원의 라운드트립 성과를 원화 환산까지 계산해 반환한다.
     */
    public List<PortfolioRoundTripResponse> compute(long memberId) {
        return compute(memberId, null);
    }

    /**
     * 소유 회원의 라운드트립을 계산한다. accountFilter가 있으면 해당 계좌로 한정한다(공유 열람).
     */
    public List<PortfolioRoundTripResponse> compute(long memberId, Long accountFilter) {
        List<PortfolioTransaction> txns = transactionService.findByOwner(memberId).stream()
                .filter(t -> accountFilter == null || accountFilter.equals(t.getAccountId()))
                .toList();
        List<PortfolioRoundTrip> trips = roundTripCalculator.fold(txns, LocalDate.now());
        Map<Long, String> accountNames = accountService.findByOwner(memberId).stream()
                .collect(Collectors.toMap(PortfolioAccount::getId, PortfolioAccount::getName));
        Map<String, BigDecimal> fxRates = fxRateService.ratesByCurrency();

        List<PortfolioRoundTripResponse> result = new ArrayList<>(trips.size());
        long id = 1;
        for (PortfolioRoundTrip t : trips) {
            BigDecimal fx = fxRates.getOrDefault(t.currency(), BigDecimal.ONE);
            long pnlKrw = t.pnlNat().multiply(fx).setScale(0, RoundingMode.HALF_UP).longValue();
            String group = accountNames.getOrDefault(t.accountId(), "?");
            result.add(PortfolioRoundTripResponse.of(id++, t, group, pnlKrw));
        }
        return result;
    }
}
