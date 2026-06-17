package com.dove.api.search.stock.controller;

import com.dove.api.global.security.authorization.RequireCapability;
import com.dove.userfeature.domain.capability.Capability;
import com.dove.api.search.stock.dto.EstimateResponse;
import com.dove.api.search.stock.dto.InvestOpinionResponse;
import com.dove.stockcollection.application.port.AnalystFetcher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * 종목 애널리스트 정보(투자의견·추정실적) on-demand 조회 API.
 */
@RestController
@RequestMapping("/stocks")
@RequireCapability(Capability.STOCK_VIEW)
public class StockAnalystController {

    private final Optional<AnalystFetcher> analystFetcher;

    @Autowired
    public StockAnalystController(@Autowired(required = false) AnalystFetcher analystFetcher) {
        this.analystFetcher = Optional.ofNullable(analystFetcher);
    }

    /**
     * 종목투자의견 — 최근 1년 회원사별 의견.
     *
     * @throws ResponseStatusException KIS 어댑터 미배포 환경에서 503
     */
    @GetMapping("/{ticker}/invest-opinion")
    public List<InvestOpinionResponse> getInvestOpinion(@PathVariable String ticker) {
        AnalystFetcher fetcher = analystFetcher.orElseThrow(
                () -> new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "ANALYST_FETCHER_UNAVAILABLE"));
        LocalDate to = LocalDate.now();
        LocalDate from = to.minusYears(1);
        return fetcher.fetchInvestOpinion(ticker, from, to).stream()
                .map(InvestOpinionResponse::from)
                .toList();
    }

    /**
     * 종목추정실적 — 리서치 커버 종목만 데이터 존재.
     *
     * @throws ResponseStatusException KIS 어댑터 미배포 환경에서 503
     */
    @GetMapping("/{ticker}/estimate")
    public EstimateResponse getEstimate(@PathVariable String ticker) {
        AnalystFetcher fetcher = analystFetcher.orElseThrow(
                () -> new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "ANALYST_FETCHER_UNAVAILABLE"));
        return fetcher.fetchEstimate(ticker)
                .map(EstimateResponse::from)
                .orElse(EstimateResponse.empty());
    }
}
