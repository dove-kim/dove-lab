package com.dove.krx.infrastructure.adapter;

import com.dove.krx.StockListing;
import com.dove.krx.TradingDayPort;
import com.dove.krx.acl.KrxListedStockTranslator;
import com.dove.krx.infrastructure.client.KrxListedStockResponse;
import com.dove.krx.infrastructure.client.KrxStockClient;
import com.dove.krx.quota.KrxAccessBlockedException;
import com.dove.krx.quota.KrxApiQuotaService;
import com.dove.krx.quota.KrxDailyQuotaExceededException;
import com.dove.krx.quota.KrxRemoteRateLimitException;
import com.dove.market.domain.enums.MarketType;
import com.dove.systemevent.application.service.SystemEventService;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * KRX Open API 상장 종목 조회 어댑터.
 * 빈 응답(미래 날짜·데이터 미제공·오류)은 빈 리스트로 반환한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class KrxTradingDayAdapter implements TradingDayPort {

    @Value("${krx.api.auth-key:}")
    private String krxApiAuthKey;

    private final KrxStockClient krxStockClient;
    private final Optional<KrxApiQuotaService> quotaService;
    private final SystemEventService systemEventService;

    @Override
    public List<StockListing> fetchListings(MarketType market, LocalDate date) {
        try {
            return fetch(market, date);
        } catch (KrxDailyQuotaExceededException | KrxRemoteRateLimitException e) {
            quotaService.ifPresent(KrxApiQuotaService::markRemoteRateLimited);
            log.error("[{}] KRX 일일 한도 초과: {}", market, date);
            systemEventService.recordKrxRateLimit(market, date, e.getMessage());
            return List.of();
        } catch (FeignException.Unauthorized e) {
            log.error("[{}] KRX 인증 오류: {}", market, date, e);
            systemEventService.recordKrxApiFailure(market, "KRX 인증 오류: " + date);
            return List.of();
        } catch (FeignException.Forbidden e) {
            // 403 Access Denied = 과도한 요청으로 인한 일시 차단. 빈 응답과 구분해 전파.
            log.error("[{}] KRX 접근 차단(403): {}", market, date);
            systemEventService.recordKrxApiFailure(market, "KRX 403 Access Denied: " + date);
            throw new KrxAccessBlockedException("KRX 403 Access Denied: " + date);
        } catch (FeignException e) {
            log.warn("[{}] KRX 종목 조회 오류: {}", market, date, e);
            return List.of();
        }
    }

    private List<StockListing> fetch(MarketType market, LocalDate date) {
        KrxListedStockResponse response = switch (market) {
            case KOSPI  -> krxStockClient.getKospiListedStocks(krxApiAuthKey, date);
            case KOSDAQ -> krxStockClient.getKosdaqListedStocks(krxApiAuthKey, date);
            case KONEX  -> krxStockClient.getKonexListedStocks(krxApiAuthKey, date);
        };
        if (response == null || response.getItems() == null) return List.of();
        return response.getItems().stream()
                .filter(item -> item.getTicker() != null && !item.getTicker().isBlank())
                .map(KrxListedStockTranslator::translate)
                .toList();
    }
}
