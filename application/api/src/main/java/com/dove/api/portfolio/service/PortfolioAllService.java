package com.dove.api.portfolio.service;

import com.dove.api.portfolio.dto.PortfolioPositionResponse;
import com.dove.api.portfolio.dto.PortfolioRoundTripResponse;
import com.dove.api.portfolio.dto.PortfolioSummaryResponse;
import com.dove.api.portfolio.dto.PortfolioTransactionResponse;
import com.dove.portfolio.application.service.PortfolioAccountService;
import com.dove.portfolio.application.service.PortfolioShareService;
import com.dove.portfolio.application.service.PortfolioTransactionService;
import com.dove.portfolio.domain.entity.PortfolioAccount;
import com.dove.portfolio.domain.enums.PortfolioSharePermission;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 접근 가능한 모든 계좌(내 소유 + 공유받은 READ/WRITE)를 합산하는 "전체" 조회 조합.
 * READ_RELATIVE 공유는 금액을 숨겨야 하므로 합산에서 제외한다.
 */
@Service
@RequiredArgsConstructor
public class PortfolioAllService {

    private final PortfolioAccountService accountService;
    private final PortfolioShareService shareService;
    private final PortfolioPositionService positionService;
    private final PortfolioSummaryService summaryService;
    private final PortfolioRoundTripService roundTripService;
    private final PortfolioTransactionService transactionService;

    /** 합산 대상 공유 계좌들 — (소유자, 계좌ID). READ_RELATIVE 제외. */
    private List<long[]> sharedAccounts(long memberId) {
        return shareService.listByGrantee(memberId).stream()
                .filter(s -> s.getPermission() != PortfolioSharePermission.READ_RELATIVE)
                .map(s -> new long[]{s.getOwnerMemberId(), s.getAccountId()})
                .toList();
    }

    /**
     * 접근 가능한 모든 계좌의 보유 포지션을 합쳐 비중을 재계산해 반환한다.
     */
    public List<PortfolioPositionResponse> positions(long memberId) {
        List<PortfolioPositionResponse> all = new ArrayList<>(positionService.compute(memberId, null));
        for (long[] a : sharedAccounts(memberId)) {
            all.addAll(positionService.compute(a[0], a[1]));
        }
        long totalEval = all.stream().mapToLong(PortfolioPositionResponse::evalKrw).sum();
        return all.stream()
                .map(p -> new PortfolioPositionResponse(p.symbol(), p.account(), p.currency(), p.tag(),
                        p.quantity(), p.avgPriceNat(), p.curPriceNat(), p.evalKrw(), p.pnlKrw(), p.pnlPct(),
                        totalEval != 0 ? Math.round(p.evalKrw() * 1000.0 / totalEval) / 10.0 : 0.0,
                        p.holdingId(), p.annualDividendPct(), p.dividendTracked()))
                .toList();
    }

    /**
     * 접근 가능한 모든 계좌를 합산한 요약. 순납입·XIRR은 내 계좌 기준(공유분은 내 납입이 아님).
     */
    public PortfolioSummaryResponse summary(long memberId) {
        PortfolioSummaryResponse own = summaryService.compute(memberId, null);
        long cash = own.cashKrw();
        Map<String, BigDecimal> cashByCurrency = new LinkedHashMap<>(own.cashByCurrency());
        for (long[] a : sharedAccounts(memberId)) {
            PortfolioSummaryResponse s = summaryService.compute(a[0], a[1]);
            cash += s.cashKrw();
            s.cashByCurrency().forEach((k, v) -> cashByCurrency.merge(k, v, BigDecimal::add));
        }
        List<PortfolioPositionResponse> positions = positions(memberId);
        long eval = positions.stream().mapToLong(PortfolioPositionResponse::evalKrw).sum();
        long pnl = positions.stream().mapToLong(PortfolioPositionResponse::pnlKrw).sum();
        long invested = eval - pnl;
        long total = cash + eval;
        long netContrib = own.netContribKrw();
        long growth = total - netContrib;
        double evalPnlPct = invested > 0 ? Math.round(pnl * 1000.0 / invested) / 10.0 : 0.0;
        return new PortfolioSummaryResponse(total, cash, netContrib, growth, pnl, evalPnlPct, own.xirrPct(), cashByCurrency);
    }

    /**
     * 접근 가능한 모든 계좌의 라운드트립을 합쳐 순번을 다시 매겨 반환한다.
     */
    public List<PortfolioRoundTripResponse> roundtrips(long memberId) {
        List<PortfolioRoundTripResponse> all = new ArrayList<>(roundTripService.compute(memberId, null));
        for (long[] a : sharedAccounts(memberId)) {
            all.addAll(roundTripService.compute(a[0], a[1]));
        }
        List<PortfolioRoundTripResponse> out = new ArrayList<>(all.size());
        long id = 1;
        for (PortfolioRoundTripResponse t : all) {
            out.add(new PortfolioRoundTripResponse(id++, t.symbol(), t.currency(), t.group(), t.entry(), t.exit(),
                    t.holdingDays(), t.avgNat(), t.exitNat(), t.pnlNat(), t.pnlKrw(), t.pnlPct(), t.open()));
        }
        return out;
    }

    /**
     * 접근 가능한 모든 계좌의 거래를 합쳐 최신순으로 반환한다.
     */
    public List<PortfolioTransactionResponse> transactions(long memberId) {
        List<PortfolioTransactionResponse> all = new ArrayList<>();
        addTransactions(all, memberId, null);
        for (long[] a : sharedAccounts(memberId)) {
            addTransactions(all, a[0], a[1]);
        }
        all.sort(Comparator.comparing(PortfolioTransactionResponse::tradedAt).reversed());
        return all;
    }

    private void addTransactions(List<PortfolioTransactionResponse> out, long ownerMemberId, Long accountFilter) {
        Map<Long, String> names = accountService.findByOwner(ownerMemberId).stream()
                .collect(Collectors.toMap(PortfolioAccount::getId, PortfolioAccount::getName));
        transactionService.findByOwner(ownerMemberId).stream()
                .filter(t -> accountFilter == null || accountFilter.equals(t.getAccountId()))
                .forEach(t -> out.add(PortfolioTransactionResponse.of(t, names.get(t.getAccountId()))));
    }
}
