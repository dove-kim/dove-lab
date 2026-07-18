package com.dove.kis.infrastructure.adapter;

import com.dove.kis.infrastructure.client.KisOverseasClient;
import com.dove.kis.infrastructure.client.dto.KisOverseasPriceResponse;
import com.dove.kis.quota.KisGate;
import com.dove.portfolio.application.port.OverseasPricePort;
import com.dove.portfolio.domain.enums.PortfolioMarket;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * KIS 해외주식 현재체결가 기반 해외 종가 포트 어댑터.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class KisOverseasPriceAdapter implements OverseasPricePort {

    private static final String TR_PRICE = "HHDFS00000300";
    private static final String AUTH = "";

    private final KisOverseasClient client;
    private final KisGate kisGate;

    @Override
    public Optional<BigDecimal> fetchClose(PortfolioMarket market, String ticker) {
        String excd = excd(market);
        if (excd == null) {
            return Optional.empty();
        }
        try {
            KisOverseasPriceResponse res = kisGate.call(() -> client.getPrice(TR_PRICE, AUTH, excd, ticker));
            if (res == null || !res.isSuccess() || res.getOutput() == null) {
                return Optional.empty();
            }
            BigDecimal last = res.getOutput().getLastDecimal();
            if (last == null || last.signum() <= 0) {
                return Optional.empty();
            }
            return Optional.of(last);
        } catch (RuntimeException e) {
            log.warn("KIS overseas price fetch failed {} {}: {}", excd, ticker, e.getMessage());
            return Optional.empty();
        }
    }

    private String excd(PortfolioMarket market) {
        return switch (market) {
            case NASDAQ -> "NAS";
            case NYSE -> "NYS";
            case AMEX -> "AMS";
            case HKEX -> "HKS";
            case TSE -> "TSE";
            case SSE -> "SHS";
            case SZSE -> "SZS";
            default -> null;
        };
    }
}
