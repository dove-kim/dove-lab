package com.dove.krx.infrastructure.adapter;

import com.dove.krx.config.KrxProperties;
import com.dove.krx.infrastructure.client.KrxDailyPriceData;
import com.dove.krx.infrastructure.client.KrxDailyPriceResponse;
import com.dove.krx.infrastructure.client.KrxStockClient;
import com.dove.krx.quota.KrxAccessBlockedException;
import com.dove.krx.quota.KrxApiQuotaService;
import com.dove.krx.quota.KrxDailyQuotaExceededException;
import com.dove.krx.quota.KrxRemoteRateLimitException;
import com.dove.stockcollection.application.port.ShareCountFetcher;
import com.dove.stockcollection.application.port.ShareCountRow;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * KRX Open API 일별시세에서 상장주식수만 취하는 어댑터. KOSPI+KOSDAQ 2콜로 전종목.
 * 빈 응답(휴장·미래·오류)은 빈 리스트로 반환한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class KrxShareCountAdapter implements ShareCountFetcher {

    private final KrxProperties props;
    private final KrxStockClient krxStockClient;
    private final Optional<KrxApiQuotaService> quotaService;

    @Override
    public List<ShareCountRow> fetch(LocalDate date) {
        List<ShareCountRow> rows = new ArrayList<>();
        collect(rows, () -> krxStockClient.getDailyKospiStockInfo(props.getAuthKey(), date), date);
        collect(rows, () -> krxStockClient.getDailyKosdaqStockInfo(props.getAuthKey(), date), date);
        return rows;
    }

    private void collect(List<ShareCountRow> out, Supplier<KrxDailyPriceResponse> call, LocalDate date) {
        KrxDailyPriceResponse response;
        try {
            response = call.get();
        } catch (KrxDailyQuotaExceededException | KrxRemoteRateLimitException e) {
            quotaService.ifPresent(KrxApiQuotaService::markRemoteRateLimited);
            log.error("KRX 상장주식수 일일 한도 초과: {}", date);
            return;
        } catch (FeignException.Forbidden e) {
            log.error("KRX 상장주식수 접근 차단(403): {}", date);
            throw new KrxAccessBlockedException("KRX 403 Access Denied: " + date);
        } catch (FeignException e) {
            log.warn("KRX 상장주식수 조회 오류: {}", date, e);
            return;
        }
        if (response == null || response.getDataList() == null) {
            return;
        }
        for (KrxDailyPriceData d : response.getDataList()) {
            String ticker = d.getStockCode();
            if (ticker == null || ticker.isBlank()) {
                continue;
            }
            try {
                long shares = d.getListedShares();
                if (shares > 0) {
                    out.add(new ShareCountRow(ticker, shares));
                }
            } catch (NumberFormatException ignored) {
                // 상장주식수 필드 결측·비정상 → 그 종목만 건너뜀
            }
        }
    }
}
