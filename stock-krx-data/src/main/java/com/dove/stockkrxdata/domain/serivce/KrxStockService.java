package com.dove.stockkrxdata.domain.serivce;

import com.dove.stockkrxdata.domain.client.KrxStockClient;
import com.dove.stockkrxdata.domain.client.KrxStockResponse;
import com.dove.stockkrxdata.domain.dto.KrxStockInfo;
import com.dove.stockkrxdata.domain.enums.MarketType;
import com.dove.stockkrxdata.repository.KrxDailyDataRepository;
import com.dove.stockkrxdata.repository.entity.KrxDailyData;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class KrxStockService {
    private final KrxStockClient krxStockClient;

    private final KrxDailyDataRepository krxDailyDataRepository;

    public List<KrxStockInfo> getStockListBy(MarketType marketType, LocalDate baseDate) {
        LocalDateTime apiCallAt = LocalDateTime.now();

        try {
            KrxStockResponse response = krxStockClient.getDailyKospiStockInfo("key", baseDate);

            if (response == null || response.getDataList() == null || response.getDataList().isEmpty()) {
                krxDailyDataRepository
                        .save(KrxDailyData.responseNull(baseDate, marketType, apiCallAt));
                return Collections.emptyList();
            }

            // 응답을 KrxStockInfo로 변환 (여기서 파싱 오류 발생 가능)
            List<KrxStockInfo> stockInfoList = response.getDataList().stream()
                    .map(data -> new KrxStockInfo(
                            data.getBaseDate(),
                            marketType,
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
            krxDailyDataRepository
                    .save(KrxDailyData.success(baseDate, marketType, response.toJson(), apiCallAt));

            return stockInfoList;

        } catch (FeignException.Unauthorized e) {
            krxDailyDataRepository
                    .save(KrxDailyData.authFailed(baseDate, marketType, apiCallAt));
            return Collections.emptyList();

        } catch (FeignException e) {
            krxDailyDataRepository
                    .save(KrxDailyData.failed(baseDate, marketType, apiCallAt));
            return Collections.emptyList();

        } catch (Exception e) {
            // 모든 파싱 오류를 포함한 예외 처리
            krxDailyDataRepository
                    .save(KrxDailyData.responseParseError(baseDate, marketType, apiCallAt));
            return Collections.emptyList();
        }
    }

}
