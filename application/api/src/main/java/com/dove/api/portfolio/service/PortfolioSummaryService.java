package com.dove.api.portfolio.service;

import com.dove.api.portfolio.dto.PortfolioPositionResponse;
import com.dove.api.portfolio.dto.PortfolioSummaryResponse;
import com.dove.portfolio.application.service.FxRateTable;
import com.dove.portfolio.application.service.PortfolioAccountService;
import com.dove.portfolio.application.service.PortfolioCashCalculator;
import com.dove.portfolio.application.service.PortfolioHoldingService;
import com.dove.portfolio.domain.entity.PortfolioHolding;
import com.dove.portfolio.application.service.PortfolioFxConversionService;
import com.dove.portfolio.application.service.PortfolioFxRateService;
import com.dove.portfolio.application.service.PortfolioTransactionService;
import com.dove.portfolio.application.service.Xirr;
import com.dove.portfolio.application.service.ExternalFlow;
import com.dove.portfolio.domain.entity.PortfolioAccount;
import com.dove.portfolio.domain.entity.PortfolioFxConversion;
import com.dove.portfolio.domain.entity.PortfolioTransaction;
import com.dove.portfolio.domain.enums.TxType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalDouble;

/**
 * 포트폴리오 요약 조합 — 통화별 현금(거래·환전 fold)과 보유 평가액을 합쳐 총자산·수익률·XIRR을 낸다.
 *
 * <p>현재 잔고와 평가액은 최신 환율로, 외부 입출금 흐름은 거래일 환율로 환산한다.</p>
 */
@Service
@RequiredArgsConstructor
public class PortfolioSummaryService {

    private static final String PERIOD_ALL = "all";

    private final PortfolioTransactionService transactionService;
    private final PortfolioPositionService positionService;
    private final PortfolioCashCalculator cashCalculator;
    private final PortfolioFxRateService fxRateService;
    private final PortfolioFxConversionService fxConversionService;
    private final PortfolioAccountService accountService;
    private final PortfolioHoldingService holdingService;
    private final PortfolioAssetSnapshotService snapshotService;

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
        // 외부 흐름은 거래일 환율로 환산 — 입금 당시 원화 투입액이 순납입이고, 환차익은 누적 손익에 남는다.
        FxRateTable fxTable = fxRateService.rateTable();
        List<ExternalFlow> flows = investorFlows(txns, fxTable);
        long netContrib = -flows.stream().mapToLong(ExternalFlow::amountKrw).sum();
        long growth = total - netContrib;
        double evalPnlPct = invested > 0 ? round1(evalPnl * 100.0 / invested) : 0.0;

        List<PortfolioHolding> holdings = holdingService.findByOwner(memberId).stream()
                .filter(h -> accountFilter == null || accountFilter.equals(h.getAccountId()))
                .toList();
        Map<String, Double> xirrByPeriod = xirrByPeriod(txns, convs, holdings, flows, fxTable, total);
        double xirrPct = xirrByPeriod.getOrDefault(PERIOD_ALL, 0.0);

        return new PortfolioSummaryResponse(total, cash, netContrib, growth, evalPnl, evalPnlPct, xirrPct,
                xirrByPeriod, cashByCurrency);
    }

    /**
     * 소유 회원의 계좌별·통화별 현금 잔고(원통화 네이티브)를 계산한다.
     */
    public Map<String, Map<String, Long>> cashByAccount(long memberId) {
        List<PortfolioTransaction> allTx = transactionService.findByOwner(memberId);
        List<PortfolioFxConversion> allConv = fxConversionService.findByOwner(memberId);
        Map<String, Map<String, Long>> result = new LinkedHashMap<>();
        for (PortfolioAccount acc : accountService.findByOwner(memberId)) {
            List<PortfolioTransaction> txns = allTx.stream().filter(t -> acc.getId().equals(t.getAccountId())).toList();
            List<PortfolioFxConversion> convs = allConv.stream().filter(c -> acc.getId().equals(c.getAccountId())).toList();
            Map<String, Long> byCur = new LinkedHashMap<>();
            cashCalculator.cashByCurrency(txns, convs).forEach((cur, amt) ->
                    byCur.put(cur, amt.setScale(0, RoundingMode.HALF_UP).longValue()));
            result.put(acc.getName(), byCur);
        }
        return result;
    }

    /** 통화별 금액을 현재 환율로 원화 환산·합산한다(KRW=×1). */
    private long toKrw(Map<String, BigDecimal> byCurrency, Map<String, BigDecimal> fxRates) {
        BigDecimal sum = BigDecimal.ZERO;
        for (Map.Entry<String, BigDecimal> e : byCurrency.entrySet()) {
            sum = sum.add(e.getValue().multiply(fxRates.getOrDefault(e.getKey(), BigDecimal.ONE)));
        }
        return sum.setScale(0, RoundingMode.HALF_UP).longValue();
    }

    /**
     * 기간별 연환산 XIRR(%)을 계산한다. 구간이 있으면 직전일 총자산을 시작 투입으로, 구간 말 총자산을 회수로 둔다.
     */
    private Map<String, Double> xirrByPeriod(List<PortfolioTransaction> txns, List<PortfolioFxConversion> convs,
                                             List<PortfolioHolding> holdings, List<ExternalFlow> allFlows,
                                             FxRateTable fxTable, long totalNow) {
        LocalDate today = LocalDate.now();
        int year = today.getYear();
        Map<String, Double> result = new LinkedHashMap<>();
        result.put(PERIOD_ALL, rate(Xirr.annualRatePct(allFlows, today, totalNow)));
        result.put("thisYear", periodRate(txns, convs, holdings, allFlows, fxTable, totalNow,
                LocalDate.of(year, 1, 1), today));
        result.put("lastYear", periodRate(txns, convs, holdings, allFlows, fxTable, totalNow,
                LocalDate.of(year - 1, 1, 1), LocalDate.of(year - 1, 12, 31)));
        result.put("last12m", periodRate(txns, convs, holdings, allFlows, fxTable, totalNow,
                today.minusYears(1), today));
        return result;
    }

    /** 한 구간 [from, to]의 XIRR(%)을 계산한다. 구간에 자산도 흐름도 없으면 0. */
    private double periodRate(List<PortfolioTransaction> txns, List<PortfolioFxConversion> convs,
                              List<PortfolioHolding> holdings, List<ExternalFlow> allFlows,
                              FxRateTable fxTable, long totalNow, LocalDate from, LocalDate to) {
        LocalDate today = LocalDate.now();
        long opening = snapshotService.totalAssetOn(txns, convs, holdings, from.minusDays(1), fxTable);
        long closing = to.isBefore(today)
                ? snapshotService.totalAssetOn(txns, convs, holdings, to, fxTable)
                : totalNow;

        List<ExternalFlow> flows = new ArrayList<>();
        if (opening != 0) {
            flows.add(new ExternalFlow(from, -opening));
        }
        allFlows.stream()
                .filter(f -> !f.date().isBefore(from) && !f.date().isAfter(to))
                .forEach(flows::add);
        if (flows.isEmpty()) {
            return 0.0;
        }
        return rate(Xirr.annualRatePct(flows, to.isBefore(today) ? to : today, closing));
    }

    private double rate(OptionalDouble value) {
        return value.isPresent() ? round1(value.getAsDouble()) : 0.0;
    }

    /** 외부 현금흐름(투자자 관점: 입금=음수, 출금=양수)을 거래일 환율로 원화 환산해 생성한다. */
    private List<ExternalFlow> investorFlows(List<PortfolioTransaction> txns, FxRateTable fxTable) {
        List<ExternalFlow> flows = new ArrayList<>();
        for (PortfolioTransaction t : txns) {
            if (t.getType() != TxType.DEPOSIT && t.getType() != TxType.WITHDRAW) {
                continue;
            }
            long krw = t.getAmount().multiply(fxTable.rateOn(t.getCurrency(), t.getTradeDate()))
                    .setScale(0, RoundingMode.HALF_UP).longValue();
            flows.add(new ExternalFlow(t.getTradeDate(), t.getType() == TxType.DEPOSIT ? -krw : krw));
        }
        return flows;
    }

    private double round1(double value) {
        return Math.round(value * 10.0) / 10.0;
    }
}
