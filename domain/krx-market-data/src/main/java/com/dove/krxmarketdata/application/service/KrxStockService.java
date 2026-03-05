package com.dove.krxmarketdata.application.service;

import com.dove.krxmarketdata.acl.KrxStockDataTranslator;
import com.dove.krxmarketdata.application.dto.KrxStockInfo;
import com.dove.krxmarketdata.domain.entity.KrxDailyData;
import com.dove.krxmarketdata.domain.repository.KrxDailyDataRepository;
import com.dove.krxmarketdata.infrastructure.client.KrxStockClient;
import com.dove.krxmarketdata.infrastructure.client.KrxStockResponse;
import com.dove.stockdata.domain.enums.MarketType;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * KRX API를 호출하여 시장별 종목 시세를 조회하는 서비스.
 * API 호출 결과를 KrxDailyData에 기록하고, 성공 시 종목 정보 리스트를 반환한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KrxStockService {
    @Value("${krx.api.auth-key}")
    private String krxApiAuthKey;

    private final KrxStockClient krxStockClient;
    private final KrxDailyDataRepository krxDailyDataRepository;

    @Transactional
    public List<KrxStockInfo> getStockListBy(MarketType marketType, LocalDate baseDate) {
        LocalDate apiCallAt = LocalDate.now();

        try {
            KrxStockResponse response = callKrxApi(marketType, baseDate);

            if (response == null || response.getDataList() == null || response.getDataList().isEmpty()) {
                krxDailyDataRepository.save(KrxDailyData.responseNull(baseDate, marketType, apiCallAt));
                return Collections.emptyList();
            }

            List<KrxStockInfo> stockInfoList = response.getDataList().stream()
                    .map(data -> KrxStockDataTranslator.translate(data, marketType))
                    .collect(Collectors.toList());

            krxDailyDataRepository.save(KrxDailyData.success(baseDate, marketType, response.toJson(), apiCallAt));

            return stockInfoList;

        } catch (UnsupportedOperationException e) {
            log.warn("Unsupported market type: {} - {}", marketType, e.getMessage());
            krxDailyDataRepository.save(KrxDailyData.unsupportedMarket(baseDate, marketType, apiCallAt));
            return Collections.emptyList();
        } catch (FeignException.Unauthorized e) {
            log.error("Feign Unauthorized Exception: {}", e.getMessage(), e);
            krxDailyDataRepository.save(KrxDailyData.authFailed(baseDate, marketType, apiCallAt));
            return Collections.emptyList();

        } catch (FeignException e) {
            log.error("Feign Exception: {}", e.getMessage(), e);
            krxDailyDataRepository.save(KrxDailyData.failed(baseDate, marketType, apiCallAt));
            return Collections.emptyList();

        } catch (Exception e) {
            log.error("General Exception (e.g., Parsing Error): {}", e.getMessage(), e);
            krxDailyDataRepository.save(KrxDailyData.responseParseError(baseDate, marketType, apiCallAt));
            return Collections.emptyList();
        }
    }

    private KrxStockResponse callKrxApi(MarketType marketType, LocalDate baseDate) {
        return switch (marketType) {
            case KOSPI -> krxStockClient.getDailyKospiStockInfo(krxApiAuthKey, baseDate);
            case KOSDAQ -> krxStockClient.getDailyKosdaqStockInfo(krxApiAuthKey, baseDate);
            case KONEX -> throw new UnsupportedOperationException("KONEX market data retrieval is not supported via KRX client.");
        };
    }
}
