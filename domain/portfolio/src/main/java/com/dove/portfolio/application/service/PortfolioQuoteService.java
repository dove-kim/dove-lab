package com.dove.portfolio.application.service;

import com.dove.portfolio.application.port.OverseasClose;
import com.dove.portfolio.application.port.OverseasPricePort;
import com.dove.portfolio.domain.entity.PortfolioHolding;
import com.dove.portfolio.domain.entity.PortfolioQuote;
import com.dove.portfolio.domain.entity.PortfolioQuoteId;
import com.dove.portfolio.domain.enums.PortfolioMarket;
import com.dove.portfolio.domain.repository.PortfolioHoldingRepository;
import com.dove.portfolio.domain.repository.PortfolioQuoteRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 해외 종목 종가 서비스 — 일자별 종가 이력을 외부 소스에서 채우고 조회한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PortfolioQuoteService {

    private final PortfolioHoldingRepository holdingRepository;
    private final PortfolioQuoteRepository quoteRepository;
    private final OverseasPricePort overseasPricePort;

    /**
     * 등록된 해외 종목의 최신 종가를 "시장|티커" 키 맵으로 반환한다(현재 평가용).
     */
    @Transactional(readOnly = true)
    public Map<String, BigDecimal> latestByKey() {
        Map<String, BigDecimal> result = new LinkedHashMap<>();
        for (PortfolioHolding h : distinctOverseas()) {
            quoteRepository.findTopByIdMarketAndIdTickerOrderByIdAsOfDesc(h.getMarket(), h.getTicker())
                    .ifPresent(q -> result.put(key(h.getMarket(), h.getTicker()), q.getClosePrice()));
        }
        return result;
    }

    /**
     * 지정일 이하 가장 최근 종가를 조회한다(과거 시점 평가용). 없으면 빈 값.
     */
    @Transactional(readOnly = true)
    public Optional<BigDecimal> closeOn(PortfolioMarket market, String ticker, LocalDate asOf) {
        return quoteRepository
                .findTopByIdMarketAndIdTickerAndIdAsOfLessThanEqualOrderByIdAsOfDesc(market, ticker, asOf)
                .map(PortfolioQuote::getClosePrice);
    }

    /**
     * 종목별 환율 이력 보유 현황처럼, 종목별로 마지막 저장일 다음날부터 오늘까지의 종가를 채운다.
     * 이력이 없는 종목은 지정 시작일부터 받는다.
     *
     * @param fallbackStart 이력이 없을 때 백필을 시작할 날짜
     * @return 새로 저장된 행 수
     */
    @Transactional
    public int refreshFrom(LocalDate fallbackStart) {
        LocalDate today = LocalDate.now();
        int saved = 0;
        for (PortfolioHolding h : distinctOverseas()) {
            LocalDate start = quoteRepository
                    .findTopByIdMarketAndIdTickerOrderByIdAsOfDesc(h.getMarket(), h.getTicker())
                    .map(q -> q.getAsOf().plusDays(1))
                    .orElse(fallbackStart);
            if (start.isAfter(today)) {
                continue;
            }
            List<OverseasClose> closes = overseasPricePort.fetchDailyCloses(h.getMarket(), h.getTicker(), start);
            if (closes.isEmpty()) {
                log.warn("Overseas daily closes empty, keeping previous: {} {} from {}",
                        h.getMarket(), h.getTicker(), start);
                continue;
            }
            for (OverseasClose c : closes) {
                quoteRepository.save(PortfolioQuote.create(h.getMarket(), h.getTicker(), c.close(), c.asOf()));
                saved++;
            }
        }
        return saved;
    }

    /**
     * 해외 종목의 현재가(원통화)를 저장 없이 조회한다(입력 검증용). 국내이거나 실패하면 빈 값.
     */
    public Optional<BigDecimal> peek(PortfolioMarket market, String ticker) {
        if (market == null || market.isDomestic() || ticker == null || ticker.isBlank()) {
            return Optional.empty();
        }
        return overseasPricePort.fetchClose(market, ticker);
    }

    /**
     * 한 해외 종목의 최신 종가를 오늘 자로 갱신한다(종목 첫 진입 시 즉시 조회용).
     *
     * @return 갱신 성공 여부(국내이거나 조회 실패면 false)
     */
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
        upsert(market, ticker, close.get(), LocalDate.now());
        return true;
    }

    private void upsert(PortfolioMarket market, String ticker, BigDecimal closePrice, LocalDate asOf) {
        quoteRepository.findById(new PortfolioQuoteId(market, ticker, asOf))
                .ifPresentOrElse(
                        q -> q.update(closePrice),
                        () -> quoteRepository.save(PortfolioQuote.create(market, ticker, closePrice, asOf)));
    }

    private String key(PortfolioMarket market, String ticker) {
        return market.name() + "|" + ticker;
    }

    private List<PortfolioHolding> distinctOverseas() {
        Map<String, PortfolioHolding> distinct = new LinkedHashMap<>();
        for (PortfolioHolding h : holdingRepository.findAll()) {
            if (h.getMarket().isDomestic()) {
                continue;
            }
            distinct.putIfAbsent(key(h.getMarket(), h.getTicker()), h);
        }
        return List.copyOf(distinct.values());
    }
}
