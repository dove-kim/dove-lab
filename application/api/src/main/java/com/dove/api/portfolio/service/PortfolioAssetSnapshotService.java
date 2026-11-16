package com.dove.api.portfolio.service;

import com.dove.portfolio.application.service.FxRateTable;
import com.dove.portfolio.application.service.PortfolioCashCalculator;
import com.dove.portfolio.application.service.PortfolioPositionCalculator;
import com.dove.portfolio.application.service.PortfolioPositionCost;
import com.dove.portfolio.application.service.PortfolioQuoteService;
import com.dove.portfolio.domain.entity.PortfolioFxConversion;
import com.dove.portfolio.domain.entity.PortfolioHolding;
import com.dove.portfolio.domain.entity.PortfolioTransaction;
import com.dove.portfolio.domain.enums.PortfolioMarket;
import com.dove.stock.application.service.StockPriceQueryService;
import com.dove.stock.domain.entity.StockPrice;
import com.dove.stock.domain.enums.PriceType;
import com.dove.stock.domain.enums.StockExchange;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 과거 임의 시점의 총자산(원화)을 거래 이력으로 복원하는 조합 서비스.
 */
@Service
@RequiredArgsConstructor
public class PortfolioAssetSnapshotService {

    private final PortfolioCashCalculator cashCalculator;
    private final PortfolioPositionCalculator positionCalculator;
    private final PortfolioQuoteService quoteService;
    private final StockPriceQueryService stockPriceQueryService;

    /**
     * 지정일 기준 총자산(현금 + 보유 평가액)을 원화로 계산한다. 그 날의 종가와 환율을 쓴다.
     *
     * @param txns     대상 거래 전체(지정일 이후 건은 내부에서 제외)
     * @param convs    대상 환전 전체(지정일 이후 건은 내부에서 제외)
     * @param holdings 종목 식별 정보(시장·티커)
     * @param asOf     기준일
     * @param fxTable  일자별 환율 조회 테이블
     */
    public long totalAssetOn(List<PortfolioTransaction> txns, List<PortfolioFxConversion> convs,
                             Collection<PortfolioHolding> holdings, LocalDate asOf, FxRateTable fxTable) {
        List<PortfolioTransaction> upTo = txns.stream()
                .filter(t -> !t.getTradeDate().isAfter(asOf))
                .toList();
        if (upTo.isEmpty()) {
            return 0L;
        }
        List<PortfolioFxConversion> convsUpTo = convs.stream()
                .filter(c -> !c.getConvDate().isAfter(asOf))
                .toList();

        long cash = toKrw(cashCalculator.cashByCurrency(upTo, convsUpTo), asOf, fxTable);
        long eval = evaluate(positionCalculator.fold(upTo), holdings, asOf, fxTable);
        return cash + eval;
    }

    /** 보유 수량에 그 날 종가·환율을 곱해 원화 평가액을 낸다. 종가를 못 구하면 원가로 둔다. */
    private long evaluate(List<PortfolioPositionCost> costs, Collection<PortfolioHolding> holdings,
                          LocalDate asOf, FxRateTable fxTable) {
        Map<String, PortfolioHolding> byKey = holdings.stream()
                .collect(Collectors.toMap(h -> key(h.getAccountId(), h.getSymbol()), h -> h, (a, b) -> a));
        Map<String, StockPrice> domestic = domesticPricesOn(holdings, asOf);

        long total = 0;
        for (PortfolioPositionCost c : costs) {
            if (c.quantity().signum() <= 0) {
                continue;
            }
            PortfolioHolding holding = byKey.get(key(c.accountId(), c.symbol()));
            String currency = holding != null ? holding.currency() : c.currency();
            BigDecimal fx = fxTable.rateOn(currency, asOf);
            BigDecimal close = closeOn(holding, asOf, domestic);
            BigDecimal nat = close != null ? close.multiply(c.quantity()) : c.investedNat();
            total += round(nat.multiply(fx));
        }
        return total;
    }

    /** 지정일 기준 종가(원통화)를 찾는다. 없으면 null. */
    private BigDecimal closeOn(PortfolioHolding holding, LocalDate asOf, Map<String, StockPrice> domestic) {
        if (holding == null) {
            return null;
        }
        PortfolioMarket market = holding.getMarket();
        if (market.isDomestic()) {
            StockPrice sp = domestic.get(holding.getTicker());
            return sp == null ? null : BigDecimal.valueOf(sp.getClosePrice());
        }
        return quoteService.closeOn(market, holding.getTicker(), asOf).orElse(null);
    }

    /** 보유 국내 종목의 지정일 이하 최신 거래일 종가를 한 번에 로드한다(key=ticker). */
    private Map<String, StockPrice> domesticPricesOn(Collection<PortfolioHolding> holdings, LocalDate asOf) {
        Set<StockExchange> exchanges = holdings.stream()
                .map(PortfolioHolding::getMarket)
                .filter(PortfolioMarket::isDomestic)
                .map(m -> StockExchange.valueOf(m.name()))
                .collect(Collectors.toSet());
        if (exchanges.isEmpty()) {
            return Map.of();
        }
        LocalDate tradeDate = stockPriceQueryService.findNthRecentTradeDateByExchanges(
                exchanges, PriceType.RAW, asOf, 0);
        return tradeDate == null ? Map.of()
                : stockPriceQueryService.findByExchangesAndDate(exchanges, PriceType.RAW, tradeDate);
    }

    private long toKrw(Map<String, BigDecimal> byCurrency, LocalDate asOf, FxRateTable fxTable) {
        BigDecimal sum = BigDecimal.ZERO;
        for (Map.Entry<String, BigDecimal> e : byCurrency.entrySet()) {
            sum = sum.add(e.getValue().multiply(fxTable.rateOn(e.getKey(), asOf)));
        }
        return round(sum);
    }

    private long round(BigDecimal value) {
        return value.setScale(0, RoundingMode.HALF_UP).longValue();
    }

    private String key(Long accountId, String symbol) {
        return accountId + "|" + symbol;
    }
}
