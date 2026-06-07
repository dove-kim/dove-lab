package com.dove.api.search.stock.controller;

import com.dove.api.global.security.authorization.RequireFeature;
import com.dove.api.search.stock.dto.EstimateResponse;
import com.dove.api.search.stock.dto.InvestOpinionResponse;
import com.dove.stockcollection.application.port.AnalystFetcher;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/**
 * 종목 애널리스트 정보(투자의견·추정실적) on-demand 조회 API.
 */
@RestController
@RequestMapping("/stocks")
@RequiredArgsConstructor
@RequireFeature("STOCK_SEARCH")
public class StockAnalystController {

    private final AnalystFetcher analystFetcher;

    /**
     * 종목투자의견 — 최근 1년 회원사별 의견.
     */
    @GetMapping("/{ticker}/invest-opinion")
    public List<InvestOpinionResponse> getInvestOpinion(@PathVariable String ticker) {
        LocalDate to = LocalDate.now();
        LocalDate from = to.minusYears(1);
        return analystFetcher.fetchInvestOpinion(ticker, from, to).stream()
                .map(InvestOpinionResponse::from)
                .toList();
    }

    /**
     * 종목추정실적 — 리서치 커버 종목만 데이터 존재.
     */
    @GetMapping("/{ticker}/estimate")
    public EstimateResponse getEstimate(@PathVariable String ticker) {
        return analystFetcher.fetchEstimate(ticker)
                .map(EstimateResponse::from)
                .orElse(EstimateResponse.empty());
    }
}
