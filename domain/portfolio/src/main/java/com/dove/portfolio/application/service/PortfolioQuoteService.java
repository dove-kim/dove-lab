package com.dove.portfolio.application.service;

import com.dove.portfolio.application.port.OverseasPricePort;
import com.dove.portfolio.domain.entity.PortfolioHolding;
import com.dove.portfolio.domain.entity.PortfolioQuote;
import com.dove.portfolio.domain.enums.PortfolioMarket;
import com.dove.portfolio.domain.repository.PortfolioHoldingRepository;
import com.dove.portfolio.domain.repository.PortfolioQuoteRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 해외 종목 종가 서비스 — 보유 해외 종목의 최신 종가를 외부 소스에서 갱신하고 조회한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PortfolioQuoteService {

    private final PortfolioHoldingRepository holdingRepository;
    private final PortfolioQuoteRepository quoteRepository;
    private final OverseasPricePort overseasPricePort;

    /**
     * 저장된 모든 해외 종가를 조회한다.
     */
    @Transactional(readOnly = true)
    public List<PortfolioQuote> findAll() {
        return quoteRepository.findAll();
    }

    /**
     * 저널에 등록된 모든 해외 종목(중복 제거)의 최신 종가를 외부 소스에서 갱신한다.
     * 조회 실패한 종목은 건너뛴다(기존값 유지).
     *
     * @return 실제로 갱신된 종목 수
     */
    @Transactional
    public int refreshOverseas() {
        int updated = 0;
        for (PortfolioHolding h : distinctOverseas()) {
            if (refreshOne(h.getMarket(), h.getTicker())) {
                updated++;
            }
        }
        return updated;
    }

    /**
     * 한 해외 종목의 최신 종가를 갱신한다(종목 첫 진입 시 즉시 조회용). 국내 시장이면 아무것도 하지 않는다.
     *
     * @return 갱신 성공 여부(국내이거나 조회 실패면 false)
     */
    /**
     * 해외 종목의 현재가(원통화)를 저장 없이 조회한다(입력 검증용). 국내이거나 실패하면 빈 값.
     */
    public Optional<BigDecimal> peek(PortfolioMarket market, String ticker) {
        if (market == null || market.isDomestic() || ticker == null || ticker.isBlank()) {
            return Optional.empty();
        }
        return overseasPricePort.fetchClose(market, ticker);
    }

    @Transactional
    public boolean refreshOne(PortfolioMarket market, String ticker) {
        if (market.isDomestic()) {
            return false;
        }
        var close = overseasPricePort.fetchClose(market, ticker);
        if (close.isEmpty()) {
            log.warn("Overseas price fetch failed, keeping previous: {} {}", market, ticker);
            return false;
        }
        upsert(market, ticker, close.get());
        return true;
    }

    private void upsert(PortfolioMarket market, String ticker, BigDecimal closePrice) {
        quoteRepository.findByMarketAndTicker(market, ticker)
                .ifPresentOrElse(
                        q -> q.update(closePrice),
                        () -> quoteRepository.save(PortfolioQuote.create(market, ticker, closePrice)));
    }

    private List<PortfolioHolding> distinctOverseas() {
        Map<String, PortfolioHolding> distinct = new LinkedHashMap<>();
        for (PortfolioHolding h : holdingRepository.findAll()) {
            if (h.getMarket().isDomestic()) {
                continue;
            }
            distinct.putIfAbsent(h.getMarket().name() + "|" + h.getTicker(), h);
        }
        return List.copyOf(distinct.values());
    }
}
