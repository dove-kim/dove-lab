package com.dove.api.portfolio.service;

import com.dove.api.portfolio.dto.PortfolioPositionResponse;
import com.dove.stock.application.service.StockPriceQueryService;
import com.dove.stock.domain.entity.StockPrice;
import com.dove.stock.domain.enums.PriceType;
import com.dove.stock.domain.enums.StockExchange;
import com.dove.portfolio.application.service.PortfolioAccountService;
import com.dove.portfolio.application.service.PortfolioFxRateService;
import com.dove.portfolio.application.service.PortfolioHoldingService;
import com.dove.portfolio.application.service.PortfolioPositionCalculator;
import com.dove.portfolio.application.service.PortfolioPositionCost;
import com.dove.portfolio.application.service.PortfolioQuoteService;
import com.dove.portfolio.application.service.PortfolioTransactionService;
import com.dove.portfolio.domain.entity.PortfolioAccount;
import com.dove.portfolio.domain.entity.PortfolioHolding;
import com.dove.portfolio.domain.entity.PortfolioQuote;
import com.dove.portfolio.domain.entity.PortfolioTransaction;
import com.dove.portfolio.domain.enums.PortfolioMarket;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 보유 포지션의 평가액·손익·비중을 계산하는 조합 서비스.
 */
@Service
@RequiredArgsConstructor
public class PortfolioPositionService {

    private final PortfolioTransactionService transactionService;
    private final PortfolioPositionCalculator positionCalculator;
    private final PortfolioHoldingService holdingService;
    private final PortfolioAccountService accountService;
    private final PortfolioFxRateService fxRateService;
    private final PortfolioQuoteService quoteService;
    private final StockPriceQueryService stockPriceQueryService;

    /**
     * 소유 회원의 보유 포지션을 평가액·손익·비중까지 계산해 반환한다.
     */
    public List<PortfolioPositionResponse> compute(long memberId) {
        return compute(memberId, null);
    }

    /**
     * 소유 회원의 보유 포지션을 계산한다. accountFilter가 있으면 해당 계좌로 한정한다(공유 열람).
     */
    public List<PortfolioPositionResponse> compute(long memberId, Long accountFilter) {
        List<PortfolioTransaction> txns = transactionService.findByOwner(memberId).stream()
                .filter(t -> accountFilter == null || accountFilter.equals(t.getAccountId()))
                .toList();
        List<PortfolioPositionCost> costs = positionCalculator.fold(txns);

        Map<String, PortfolioHolding> holdings = holdingService.findByOwner(memberId).stream()
                .filter(h -> accountFilter == null || accountFilter.equals(h.getAccountId()))
                .collect(Collectors.toMap(h -> key(h.getAccountId(), h.getSymbol()), Function.identity(), (a, b) -> a));
        Map<Long, String> accountNames = accountService.findByOwner(memberId).stream()
                .collect(Collectors.toMap(PortfolioAccount::getId, PortfolioAccount::getName));
        Map<String, BigDecimal> fxRates = fxRateService.ratesByCurrency();
        Map<String, BigDecimal> quotes = quoteService.findAll().stream()
                .collect(Collectors.toMap(q -> key(q.getMarket(), q.getTicker()), PortfolioQuote::getClosePrice));
        // 국내 현재가는 보유 거래소의 최신 거래일 전 종목을 한 번에 로드(종목별 N+1 조회 회피). key=ticker.
        Map<String, StockPrice> domesticPrices = domesticPrices(holdings.values());

        List<ResolvedPosition> resolved = costs.stream()
                .map(c -> resolve(c, holdings.get(key(c.accountId(), c.symbol())),
                        accountNames.get(c.accountId()), fxRates, quotes, domesticPrices))
                .toList();

        long totalEval = resolved.stream().mapToLong(ResolvedPosition::evalKrw).sum();
        return resolved.stream().map(r -> toResponse(r, totalEval)).toList();
    }

    private ResolvedPosition resolve(PortfolioPositionCost cost, PortfolioHolding holding, String account,
                                     Map<String, BigDecimal> fxRates, Map<String, BigDecimal> quotes,
                                     Map<String, StockPrice> domesticPrices) {
        String currency = holding != null ? holding.currency() : cost.currency();
        // 원통화 원가·평가를 현재 환율로 원화 환산(FX가 원가·평가 양쪽에 같은 오늘 환율로 적용 → 수익률은 원통화 기준).
        BigDecimal fx = fxRates.getOrDefault(currency, BigDecimal.ONE);
        long investedKrw = round(cost.investedNat().multiply(fx));
        Long holdingId = holding != null ? holding.getId() : null;
        Double divPct = holding != null ? holding.getAnnualDividendPct() : null;
        boolean tracked = holding != null && holding.isDividendTracked();
        BigDecimal curPrice = currentPrice(holding, currency, fxRates, quotes, domesticPrices);
        if (curPrice == null) {
            // 현재가 미연동/미확보 → 원가로 표시(손익 0)
            return new ResolvedPosition(account, cost.symbol(), currency, cost.quantity(), cost.avgPriceNat(),
                    cost.avgPriceNat(), investedKrw, investedKrw, holdingId, divPct, tracked);
        }
        long evalKrw = round(curPrice.multiply(cost.quantity()).multiply(fx));
        return new ResolvedPosition(account, cost.symbol(), currency, cost.quantity(), cost.avgPriceNat(),
                curPrice, evalKrw, investedKrw, holdingId, divPct, tracked);
    }

    /**
     * 현재가(원통화)를 조회한다. 국내는 사전 로드한 종가 맵, 해외는 PortfolioQuote. 없으면 null.
     */
    private BigDecimal currentPrice(PortfolioHolding holding, String currency,
                                    Map<String, BigDecimal> fxRates, Map<String, BigDecimal> quotes,
                                    Map<String, StockPrice> domesticPrices) {
        if (holding == null) {
            return null;
        }
        PortfolioMarket market = holding.getMarket();
        if (market.isDomestic()) {
            StockPrice sp = domesticPrices.get(holding.getTicker());
            return sp == null ? null : BigDecimal.valueOf(sp.getClosePrice());
        }
        // 해외 — 환율이 있어야 원화 평가가 가능
        if (!fxRates.containsKey(currency)) {
            return null;
        }
        return quotes.get(key(market, holding.getTicker()));
    }

    /**
     * 보유 국내 종목의 최신 종가를 한 번에 로드한다(거래소 최신 거래일 기준, key=ticker).
     */
    private Map<String, StockPrice> domesticPrices(java.util.Collection<PortfolioHolding> holdings) {
        Set<StockExchange> exchanges = holdings.stream()
                .map(PortfolioHolding::getMarket)
                .filter(PortfolioMarket::isDomestic)
                .map(m -> StockExchange.valueOf(m.name()))
                .collect(Collectors.toSet());
        if (exchanges.isEmpty()) {
            return Map.of();
        }
        LocalDate latest = stockPriceQueryService.findNthRecentTradeDateByExchanges(
                exchanges, PriceType.RAW, LocalDate.now(), 0);
        return latest == null ? Map.of()
                : stockPriceQueryService.findByExchangesAndDate(exchanges, PriceType.RAW, latest);
    }

    private PortfolioPositionResponse toResponse(ResolvedPosition r, long totalEval) {
        long pnlKrw = r.evalKrw() - r.investedKrw();
        double pnlPct = r.investedKrw() != 0 ? round1(pnlKrw * 100.0 / r.investedKrw()) : 0.0;
        double weightPct = totalEval != 0 ? round1(r.evalKrw() * 100.0 / totalEval) : 0.0;
        return new PortfolioPositionResponse(r.symbol(), r.account(), r.currency(), null, r.quantity(),
                r.avgPriceNat(), r.curPriceNat(), r.evalKrw(), pnlKrw, pnlPct, weightPct, r.holdingId(),
                r.annualDividendPct(), r.dividendTracked());
    }

    private long round(BigDecimal value) {
        return value.setScale(0, RoundingMode.HALF_UP).longValue();
    }

    private double round1(double value) {
        return Math.round(value * 10.0) / 10.0;
    }

    private String key(Long accountId, String symbol) {
        return accountId + "|" + symbol;
    }

    private String key(PortfolioMarket market, String ticker) {
        return market.name() + "|" + ticker;
    }
}
