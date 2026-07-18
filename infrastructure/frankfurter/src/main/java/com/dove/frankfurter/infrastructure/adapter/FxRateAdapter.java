package com.dove.frankfurter.infrastructure.adapter;

import com.dove.frankfurter.infrastructure.client.FrankfurterClient;
import com.dove.frankfurter.infrastructure.client.FrankfurterResponse;
import com.dove.portfolio.application.port.FxQuote;
import com.dove.portfolio.application.port.FxRatePort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * Frankfurter 기반 환율 포트 어댑터.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FxRateAdapter implements FxRatePort {

    private static final String KRW = "KRW";

    private final FrankfurterClient client;

    @Override
    public Optional<FxQuote> fetchRateToKrw(String currency) {
        try {
            FrankfurterResponse res = client.latest(currency, KRW);
            if (res == null || res.rates() == null) {
                return Optional.empty();
            }
            BigDecimal rate = res.rates().get(KRW);
            if (rate == null) {
                return Optional.empty();
            }
            return Optional.of(new FxQuote(currency, rate, res.date()));
        } catch (RuntimeException e) {
            log.warn("Frankfurter fetch failed for {}: {}", currency, e.getMessage());
            return Optional.empty();
        }
    }
}
