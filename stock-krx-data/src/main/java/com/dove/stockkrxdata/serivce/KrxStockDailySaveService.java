package com.dove.stockkrxdata.serivce;

import com.dove.stockkrxdata.dto.KrxStockInfo;
import com.dove.stockkrxdata.enums.KrxMarketType;
import com.dove.stockdata.enums.MarketType;
import com.dove.stockdata.service.StockDataSaveService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
@AllArgsConstructor
public class KrxStockDailySaveService {
    private final KrxStockService krxStockService;
    private final StockDataSaveService stockDataSaveService;

    /**
     * 한국거래소를 통해 주식 정보를 조회 후 저장한다.
     *
     * @param marketType 시장 타입
     * @param baseDate   조회 날짜
     */
    public void saveKrxDailyStockData(MarketType marketType, LocalDate baseDate) {
        List<KrxStockInfo> krxStockInfoList = krxStockService
                .getStockListBy(KrxMarketType.of(marketType), baseDate);

        // 주식 정보 저장
        krxStockInfoList
                .forEach(krxStockInfo -> stockDataSaveService
                        .update(
                                krxStockInfo.tradingDate(),
                                krxStockInfo.krxMarketType().toMarketType(),
                                krxStockInfo.stockCode(),
                                krxStockInfo.stockName(),
                                krxStockInfo.tradingVolume(),
                                krxStockInfo.openingPrice(),
                                krxStockInfo.closingPrice(),
                                krxStockInfo.lowestPrice(),
                                krxStockInfo.highestPrice()
                        )
                );
    }
}
