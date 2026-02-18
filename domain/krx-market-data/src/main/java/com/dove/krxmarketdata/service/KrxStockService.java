package com.dove.krxmarketdata.service;

import com.dove.krxmarketdata.client.KrxStockClient;
import com.dove.krxmarketdata.client.KrxStockResponse;
import com.dove.krxmarketdata.dto.KrxStockInfo;
import com.dove.krxmarketdata.entity.KrxDailyData;
import com.dove.krxmarketdata.repository.KrxDailyDataRepository;
import com.dove.stockdata.enums.MarketType;
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
                    .map(data -> new KrxStockInfo(
                            data.getBaseDate(),
                            marketType,
                            data.getStockName(),
                            data.getStockCode(),
                            data.getTradingVolume(),
                            data.getOpeningPrice(),
                            data.getClosingPrice(),
                            data.getLowPrice(),
                            data.getHighPrice()
                    ))
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
