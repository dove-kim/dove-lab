package com.dove.portfolio.application.service;

import com.dove.portfolio.application.port.FxQuote;
import com.dove.portfolio.application.port.FxRatePort;
import com.dove.portfolio.domain.entity.PortfolioFxRate;
import com.dove.portfolio.domain.entity.PortfolioFxRateId;
import com.dove.portfolio.domain.enums.PortfolioMarket;
import com.dove.portfolio.domain.repository.PortfolioFxRateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 평가용 원통화 환율 서비스 — 외부 소스에서 일자별 이력을 채우고 조회한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PortfolioFxRateService {

    private static final String KRW = "KRW";

    private final PortfolioFxRateRepository repository;
    private final FxRatePort fxRatePort;

    /**
     * 통화 코드 → 최신 원화 환율 맵을 반환한다(KRW=1 포함).
     */
    @Transactional(readOnly = true)
    public Map<String, BigDecimal> ratesByCurrency() {
        Map<String, BigDecimal> rates = new LinkedHashMap<>();
        for (String currency : foreignCurrencies()) {
            repository.findTopByIdCurrencyOrderByIdRateDateDesc(currency)
                    .ifPresent(r -> rates.put(currency, r.getRate()));
        }
        rates.put(KRW, BigDecimal.ONE);
        return rates;
    }

    /**
     * 일자별 환율 조회 테이블을 만든다(과거 거래의 원화 환산용).
     */
    @Transactional(readOnly = true)
    public FxRateTable rateTable() {
        return FxRateTable.of(repository.findByIdCurrencyIn(foreignCurrencies()));
    }

    /**
     * 통화별 환율 이력 현황(최신 고시일·환율·보유 행수)을 반환한다.
     */
    @Transactional(readOnly = true)
    public List<FxRateCoverage> coverage() {
        Map<String, List<PortfolioFxRate>> byCurrency = repository.findByIdCurrencyIn(foreignCurrencies()).stream()
                .collect(Collectors.groupingBy(PortfolioFxRate::getCurrency));
        return foreignCurrencies().stream()
                .map(currency -> FxRateCoverage.of(currency, byCurrency.getOrDefault(currency, List.of())))
                .toList();
    }

    /**
     * 최근 거래일 기준으로 환율 이력을 조회한다(고시일 내림차순).
     *
     * @param days 거슬러 올라갈 고시일 수
     */
    @Transactional(readOnly = true)
    public List<PortfolioFxRate> recentRates(int days) {
        Set<String> currencies = foreignCurrencies();
        List<PortfolioFxRate> rows = repository.findByIdCurrencyInOrderByIdRateDateDesc(
                currencies, PageRequest.of(0, days * currencies.size()));
        Set<LocalDate> kept = rows.stream()
                .map(PortfolioFxRate::getRateDate)
                .distinct()
                .limit(days)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        return rows.stream().filter(r -> kept.contains(r.getRateDate())).toList();
    }

    /**
     * 통화별로 마지막 저장일 다음날부터 오늘까지의 환율을 채운다. 이력이 없는 통화는 지정 시작일부터 받는다.
     *
     * @param fallbackStart 이력이 없을 때 백필을 시작할 날짜
     * @return 새로 저장된 행 수
     */
    @Transactional
    public int refreshFrom(LocalDate fallbackStart) {
        LocalDate today = LocalDate.now();
        int saved = 0;
        for (String currency : foreignCurrencies()) {
            LocalDate start = repository.findTopByIdCurrencyOrderByIdRateDateDesc(currency)
                    .map(r -> r.getRateDate().plusDays(1))
                    .orElse(fallbackStart);
            if (start.isAfter(today)) {
                continue;
            }
            List<FxQuote> quotes = fxRatePort.fetchSeriesToKrw(currency, start, today);
            if (quotes.isEmpty()) {
                log.warn("FX series empty, keeping previous: {} {}..{}", currency, start, today);
                continue;
            }
            for (FxQuote q : quotes) {
                repository.save(PortfolioFxRate.create(q.currency(), q.rateToKrw(), q.rateDate()));
                saved++;
            }
        }
        return saved;
    }

    /**
     * 한 원통화의 최신 환율을 갱신한다(종목 첫 진입 시 즉시 조회용).
     *
     * @return 갱신 성공 여부(조회 실패면 false, 기존값 유지)
     */
    @Transactional
    public boolean refreshOne(String currency) {
        var quote = fxRatePort.fetchRateToKrw(currency);
        if (quote.isEmpty()) {
            log.warn("FX rate fetch failed, keeping previous: {}", currency);
            return false;
        }
        upsert(currency, quote.get().rateToKrw(), quote.get().rateDate());
        return true;
    }

    private void upsert(String currency, BigDecimal rate, LocalDate rateDate) {
        repository.findById(new PortfolioFxRateId(currency, rateDate))
                .ifPresentOrElse(
                        f -> f.update(rate),
                        () -> repository.save(PortfolioFxRate.create(currency, rate, rateDate)));
    }

    private Set<String> foreignCurrencies() {
        return Arrays.stream(PortfolioMarket.values())
                .filter(m -> !m.isDomestic())
                .map(PortfolioMarket::getCurrency)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }
}
