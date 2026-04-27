package com.dove.krx.infrastructure.adapter;

import com.dove.krx.acl.KrxDailyStockPriceTranslator;
import com.dove.krxcalllog.application.service.KrxDailyDataAuditor;
import com.dove.krxcalllog.domain.entity.KrxDailyData;
import com.dove.krxcalllog.domain.repository.KrxDailyDataRepository;
import com.dove.krx.infrastructure.client.KrxDailyPriceResponse;
import com.dove.krx.infrastructure.client.KrxStockClient;
import com.dove.stockprice.application.port.DailyPriceFetcher;
import com.dove.stockprice.application.port.StockInfo;
import com.dove.market.domain.enums.MarketType;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.stream.Collectors;

/** KRX Open API 기반 DailyPriceFetcher 구현. T+2 09:00 KST 이전 빈 응답은 RetryLater(UNCERTAIN). */
@Slf4j
@Component
@RequiredArgsConstructor
public class KrxDailyPriceFetcher implements DailyPriceFetcher {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    /** 확정 가능 시점: targetDate 이후 2일째 오전 9시(KST)까지는 빈 응답을 UNCERTAIN으로 본다. */
    private static final long CONFIRMATION_DELAY_DAYS = 2;
    private static final int CONFIRMATION_HOUR_KST = 9;
    private static final long TRANSIENT_BACKOFF_SECONDS = 300;

    @Value("${krx.api.auth-key}")
    private String krxApiAuthKey;

    private final KrxStockClient krxStockClient;
    private final KrxDailyDataRepository krxDailyDataRepository;
    private final KrxDailyDataAuditor krxDailyDataAuditor;
    private final Clock clock;

    @Override
    public Outcome fetchDailyMarketData(MarketType marketType, LocalDate baseDate) {
        LocalDateTime apiCallAt = LocalDateTime.now(clock);

        KrxDailyPriceResponse response;
        try {
            response = callKrxApi(marketType, baseDate);
        } catch (FeignException.Unauthorized e) {
            log.error("Feign Unauthorized: {}", e.getMessage(), e);
            krxDailyDataAuditor.recordFailure(KrxDailyData.authFailed(baseDate, marketType, apiCallAt));
            return new Outcome.PermanentFail(Reason.AUTH_FAILED, e.getMessage());
        } catch (FeignException e) {
            log.error("Feign Exception: {}", e.getMessage(), e);
            krxDailyDataAuditor.recordFailure(KrxDailyData.failed(baseDate, marketType, apiCallAt));
            return new Outcome.RetryLater(
                    Reason.TRANSIENT,
                    clock.instant().plusSeconds(TRANSIENT_BACKOFF_SECONDS),
                    e.getMessage());
        }

        if (response == null || response.getDataList() == null) {
            krxDailyDataAuditor.recordFailure(KrxDailyData.responseNull(baseDate, marketType, apiCallAt));
            return new Outcome.PermanentFail(Reason.PARSE_FAILED,
                    "KRX response body null for market=" + marketType + " date=" + baseDate);
        }

        if (response.getDataList().isEmpty()) {
            krxDailyDataRepository.save(KrxDailyData.responseNull(baseDate, marketType, apiCallAt));
            return isPastConfirmationTime(baseDate)
                    ? new Outcome.Holiday()
                    : new Outcome.RetryLater(Reason.UNCERTAIN, confirmationInstant(baseDate),
                            "empty response before confirmation time");
        }

        List<StockInfo> stockInfoList;
        try {
            stockInfoList = response.getDataList().stream()
                    .map(data -> KrxDailyStockPriceTranslator.translate(data, marketType))
                    .collect(Collectors.toList());
        } catch (RuntimeException e) {
            log.error("Response parse error: {}", e.getMessage(), e);
            krxDailyDataAuditor.recordFailure(KrxDailyData.responseParseError(baseDate, marketType, apiCallAt));
            return new Outcome.PermanentFail(Reason.PARSE_FAILED, e.getMessage());
        }

        krxDailyDataRepository.save(KrxDailyData.success(baseDate, marketType, response.toJson(), apiCallAt));
        return new Outcome.Success(stockInfoList);
    }

    private boolean isPastConfirmationTime(LocalDate targetDate) {
        return !clock.instant().isBefore(confirmationInstant(targetDate));
    }

    private Instant confirmationInstant(LocalDate targetDate) {
        return targetDate.plusDays(CONFIRMATION_DELAY_DAYS)
                .atTime(CONFIRMATION_HOUR_KST, 0)
                .atZone(KST)
                .toInstant();
    }

    private KrxDailyPriceResponse callKrxApi(MarketType marketType, LocalDate baseDate) {
        return switch (marketType) {
            case KOSPI -> krxStockClient.getDailyKospiStockInfo(krxApiAuthKey, baseDate);
            case KOSDAQ -> krxStockClient.getDailyKosdaqStockInfo(krxApiAuthKey, baseDate);
            case KONEX -> krxStockClient.getDailyKonexStockInfo(krxApiAuthKey, baseDate);
        };
    }
}
