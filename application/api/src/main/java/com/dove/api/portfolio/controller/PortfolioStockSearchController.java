package com.dove.api.portfolio.controller;

import com.dove.api.global.security.authorization.RequireCapability;
import com.dove.api.portfolio.dto.OverseasQuoteResponse;
import com.dove.portfolio.application.service.PortfolioQuoteService;
import com.dove.portfolio.domain.enums.PortfolioMarket;
import com.dove.stock.application.dto.StockSearchHit;
import com.dove.stock.application.service.StockQueryService;
import com.dove.userfeature.domain.capability.Capability;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;

/**
 * 매매일지 종목 입력 도우미 — 국내 종목 검색 + 해외 종목 시세 검증.
 */
@RestController
@RequestMapping("/portfolio/stock-search")
@RequiredArgsConstructor
@RequireCapability(Capability.PORTFOLIO_LEDGER)
public class PortfolioStockSearchController {

    private final StockQueryService stockQueryService;
    private final PortfolioQuoteService quoteService;

    @GetMapping
    public List<StockSearchHit> search(@RequestParam(name = "q", required = false) String q) {
        return stockQueryService.search(q, 15);
    }

    /**
     * 해외 종목의 현재가를 조회해 시장·티커가 유효한지 검증한다.
     */
    @GetMapping("/overseas")
    public OverseasQuoteResponse verifyOverseas(@RequestParam PortfolioMarket market, @RequestParam String ticker) {
        var price = quoteService.peek(market, ticker);
        return new OverseasQuoteResponse(price.isPresent(), price.map(BigDecimal::doubleValue).orElse(null));
    }
}
