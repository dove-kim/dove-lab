package com.dove.krx.infrastructure.adapter;

import com.dove.krx.infrastructure.client.KrxListedStockResponse;
import com.dove.krx.infrastructure.client.KrxStockClient;
import com.dove.stock.application.port.StockListingFetcher;
import com.dove.market.domain.enums.MarketType;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * KRX Feign 클라이언트 기반 StockListingFetcher 구현.
 * 모든 결과를 Outcome으로 분류. 소비자가 재시도/DLQ 정책을 결정한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class KrxStockListingFetcher implements StockListingFetcher {

    private static final long TRANSIENT_BACKOFF_SECONDS = 300;

    @Value("${krx.api.auth-key}")
    private String krxApiAuthKey;

    private final KrxStockClient krxStockClient;
    private final Clock clock;

    @Override
    public Outcome fetch(MarketType market, LocalDate date) {
        KrxListedStockResponse response;
        try {
            response = callKrxApi(market, date);
        } catch (FeignException.Unauthorized e) {
            log.error("Feign Unauthorized: {}", e.getMessage(), e);
            return new Outcome.PermanentFail(Reason.AUTH_FAILED, e.getMessage());
        } catch (FeignException e) {
            log.error("Feign Exception: {}", e.getMessage(), e);
            return new Outcome.RetryLater(
                    Reason.TRANSIENT,
                    clock.instant().plusSeconds(TRANSIENT_BACKOFF_SECONDS),
                    e.getMessage());
        }

        if (response == null || response.getItems() == null) {
            return new Outcome.PermanentFail(Reason.PARSE_FAILED,
                    "KRX listing response body null for market=%s date=%s".formatted(market, date));
        }
        if (response.getItems().isEmpty()) {
            return new Outcome.Holiday();
        }

        Map<String, String> stocks = response.getItems().stream()
                .collect(Collectors.toMap(
                        KrxListedStockResponse.Item::getStockCode,
                        KrxListedStockResponse.Item::getStockName,
                        (existing, duplicate) -> existing
                ));
        return new Outcome.Success(stocks);
    }

    private KrxListedStockResponse callKrxApi(MarketType market, LocalDate date) {
        return switch (market) {
            case KOSPI -> krxStockClient.getKospiListedStocks(krxApiAuthKey, date);
            case KOSDAQ -> krxStockClient.getKosdaqListedStocks(krxApiAuthKey, date);
            case KONEX -> krxStockClient.getKonexListedStocks(krxApiAuthKey, date);
        };
    }
}
