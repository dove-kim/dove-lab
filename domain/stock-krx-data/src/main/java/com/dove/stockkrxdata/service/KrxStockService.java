package com.dove.stockkrxdata.service;

import com.dove.stockkrxdata.client.KrxStockClient;
import com.dove.stockkrxdata.client.KrxStockResponse;
import com.dove.stockkrxdata.dto.KrxStockInfo;
import com.dove.stockkrxdata.entity.KrxDailyData;
import com.dove.stockkrxdata.enums.KrxMarketType;
import com.dove.stockkrxdata.repository.KrxDailyDataRepository;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.function.BiFunction;
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
    public List<KrxStockInfo> getStockListBy(KrxMarketType krxMarketType, LocalDate baseDate) {
        LocalDateTime apiCallAt = LocalDateTime.now();
        KrxStockResponse response = null;

        try {
            BiFunction<String, LocalDate, KrxStockResponse> clientMethod = krxMarketType.getClientMethod(krxStockClient);
            response = clientMethod.apply(krxApiAuthKey, baseDate);

            if (response == null || response.getDataList() == null || response.getDataList().isEmpty()) {
                krxDailyDataRepository.save(KrxDailyData.responseNull(baseDate, krxMarketType, apiCallAt));
                return Collections.emptyList();
            }

            // 응답을 KrxStockInfo로 변환
            List<KrxStockInfo> stockInfoList = response.getDataList().stream()
                    .map(data -> new KrxStockInfo(
                            data.getBaseDate(),
                            krxMarketType, // 전달받은 marketType 사용
                            data.getStockName(),
                            data.getStockCode(),
                            data.getTradingVolume().longValue(),
                            data.getOpeningPrice(),
                            data.getClosingPrice(),
                            data.getLowPrice(),
                            data.getHighPrice()
                    ))
                    .collect(Collectors.toList());

            // 성공적으로 데이터를 처리한 경우
            krxDailyDataRepository.save(KrxDailyData.success(baseDate, krxMarketType, response.toJson(), apiCallAt));

            return stockInfoList;

        } catch (UnsupportedOperationException e) {
            // KrxMarketType의 getClientMethod에서 발생한 예외 처리 (예: KONEX)
            log.warn("Unsupported market type: {} - {}", krxMarketType, e.getMessage());
            krxDailyDataRepository.save(KrxDailyData.unsupportedMarket(baseDate, krxMarketType, apiCallAt));
            return Collections.emptyList();
        } catch (FeignException.Unauthorized e) {
            log.error("Feign Unauthorized Exception: {}", e.getMessage(), e);
            krxDailyDataRepository.save(KrxDailyData.authFailed(baseDate, krxMarketType, apiCallAt));
            return Collections.emptyList();

        } catch (FeignException e) {
            log.error("Feign Exception: {}", e.getMessage(), e);
            krxDailyDataRepository.save(KrxDailyData.failed(baseDate, krxMarketType, apiCallAt));
            return Collections.emptyList();

        } catch (Exception e) {
            // 모든 파싱 오류를 포함한 예외 처리
            log.error("General Exception (e.g., Parsing Error): {}", e.getMessage(), e);
            krxDailyDataRepository.save(KrxDailyData.responseParseError(baseDate, krxMarketType, apiCallAt));
            return Collections.emptyList();
        }
    }
}
