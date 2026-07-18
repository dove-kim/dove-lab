package com.dove.portfolio.application.service;

import com.dove.portfolio.application.port.FxRatePort;
import com.dove.portfolio.domain.entity.PortfolioFxRate;
import com.dove.portfolio.domain.enums.PortfolioMarket;
import com.dove.portfolio.domain.repository.PortfolioFxRateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 평가용 원통화 환율 서비스 — 외부 소스에서 갱신하고 최신값을 조회한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PortfolioFxRateService {

    private final PortfolioFxRateRepository repository;
    private final FxRatePort fxRatePort;

    /**
     * 저장된 모든 원통화 환율을 조회한다.
     */
    @Transactional(readOnly = true)
    public List<PortfolioFxRate> findAll() {
        return repository.findAll();
    }

    /**
     * 통화 코드 → 원화 환율 맵을 반환한다(KRW=1 포함).
     */
    @Transactional(readOnly = true)
    public Map<String, BigDecimal> ratesByCurrency() {
        Map<String, BigDecimal> rates = repository.findAll().stream()
                .collect(Collectors.toMap(PortfolioFxRate::getCurrency, PortfolioFxRate::getRate));
        rates.put("KRW", BigDecimal.ONE);
        return rates;
    }

    /**
     * 저널이 다루는 모든 외화(KRW 제외)의 최신 환율을 외부 소스에서 갱신한다.
     * 특정 통화 조회 실패 시 그 통화는 건너뛴다(기존값 유지).
     *
     * @return 실제로 갱신된 통화 수
     */
    @Transactional
    public int refreshAll() {
        int updated = 0;
        for (String currency : foreignCurrencies()) {
            if (refreshOne(currency)) {
                updated++;
            }
        }
        return updated;
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
        repository.findById(currency)
                .ifPresentOrElse(
                        f -> f.update(rate, rateDate),
                        () -> repository.save(PortfolioFxRate.create(currency, rate, rateDate)));
    }

    private Set<String> foreignCurrencies() {
        return Arrays.stream(PortfolioMarket.values())
                .filter(m -> !m.isDomestic())
                .map(PortfolioMarket::getCurrency)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }
}
