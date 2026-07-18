package com.dove.api.portfolio.service;

import com.dove.api.portfolio.dto.PortfolioPositionResponse;
import com.dove.api.portfolio.dto.PortfolioSummaryResponse;
import com.dove.portfolio.application.service.PortfolioCashCalculator;
import com.dove.portfolio.application.service.PortfolioFxConversionService;
import com.dove.portfolio.application.service.PortfolioFxRateService;
import com.dove.portfolio.application.service.PortfolioTransactionService;
import com.dove.portfolio.application.service.Xirr;
import com.dove.portfolio.application.service.ExternalFlow;
import com.dove.portfolio.domain.entity.PortfolioFxConversion;
import com.dove.portfolio.domain.entity.PortfolioTransaction;
import com.dove.portfolio.domain.enums.TxType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.OptionalDouble;

/**
 * 포트폴리오 요약 조합 — 통화별 현금(거래·환전 fold)을 현재 환율로 원화 환산하고 보유 평가액을 합쳐 총자산·수익률·XIRR을 낸다.
 */
@Service
@RequiredArgsConstructor
public class PortfolioSummaryService {

    private final PortfolioTransactionService transactionService;
    private final PortfolioPositionService positionService;
    private final PortfolioCashCalculator cashCalculator;
    private final PortfolioFxRateService fxRateService;
    private final PortfolioFxConversionService fxConversionService;

    /**
     * 소유 회원의 포트폴리오 요약을 계산한다.
     */
    public PortfolioSummaryResponse compute(long memberId) {
        return compute(memberId, null);
    }

    /**
     * 소유 회원의 포트폴리오 요약을 계산한다. accountFilter가 있으면 해당 계좌로 한정한다(공유 열람).
     */
    public PortfolioSummaryResponse compute(long memberId, Long accountFilter) {
        List<PortfolioTransaction> txns = transactionService.findByOwner(memberId).stream()
                .filter(t -> accountFilter == null || accountFilter.equals(t.getAccountId()))
                .toList();
        List<PortfolioFxConversion> convs = fxConversionService.findByOwner(memberId).stream()
                .filter(c -> accountFilter == null || accountFilter.equals(c.getAccountId()))
                .toList();
        Map<String, BigDecimal> fxRates = fxRateService.ratesByCurrency();

        Map<String, BigDecimal> cashByCurrency = cashCalculator.cashByCurrency(txns, convs);
        long cash = toKrw(cashByCurrency, fxRates);

        List<PortfolioPositionResponse> positions = positionService.compute(memberId, accountFilter);
        long eval = positions.stream().mapToLong(PortfolioPositionResponse::evalKrw).sum();
        long invested = positions.stream().mapToLong(p -> p.evalKrw() - p.pnlKrw()).sum();
        long evalPnl = positions.stream().mapToLong(PortfolioPositionResponse::pnlKrw).sum();

        long total = cash + eval;
        long deposits = toKrw(cashCalculator.depositsByCurrency(txns), fxRates);
        long withdrawals = toKrw(cashCalculator.withdrawalsByCurrency(txns), fxRates);
        long netContrib = deposits - withdrawals;
        long growth = total - netContrib;
        double evalPnlPct = invested > 0 ? round1(evalPnl * 100.0 / invested) : 0.0;

        OptionalDouble xirr = Xirr.annualRatePct(investorFlows(txns, fxRates), LocalDate.now(), total);
        double xirrPct = xirr.isPresent() ? round1(xirr.getAsDouble()) : 0.0;

        return new PortfolioSummaryResponse(total, cash, netContrib, growth, evalPnl, evalPnlPct, xirrPct, cashByCurrency);
    }

    /** 통화별 금액을 현재 환율로 원화 환산·합산한다(KRW=×1). */
    private long toKrw(Map<String, BigDecimal> byCurrency, Map<String, BigDecimal> fxRates) {
        BigDecimal sum = BigDecimal.ZERO;
        for (Map.Entry<String, BigDecimal> e : byCurrency.entrySet()) {
            sum = sum.add(e.getValue().multiply(fxRates.getOrDefault(e.getKey(), BigDecimal.ONE)));
        }
        return sum.setScale(0, RoundingMode.HALF_UP).longValue();
    }

    /** 외부 현금흐름(투자자 관점: 입금=음수, 출금=양수)을 현재 환율로 원화 환산해 생성한다. */
    private List<ExternalFlow> investorFlows(List<PortfolioTransaction> txns, Map<String, BigDecimal> fxRates) {
        List<ExternalFlow> flows = new ArrayList<>();
        for (PortfolioTransaction t : txns) {
            if (t.getType() != TxType.DEPOSIT && t.getType() != TxType.WITHDRAW) {
                continue;
            }
            long krw = t.getAmount().multiply(fxRates.getOrDefault(t.getCurrency(), BigDecimal.ONE))
                    .setScale(0, RoundingMode.HALF_UP).longValue();
            flows.add(new ExternalFlow(t.getTradeDate(), t.getType() == TxType.DEPOSIT ? -krw : krw));
        }
        return flows;
    }

    private double round1(double value) {
        return Math.round(value * 10.0) / 10.0;
    }
}
