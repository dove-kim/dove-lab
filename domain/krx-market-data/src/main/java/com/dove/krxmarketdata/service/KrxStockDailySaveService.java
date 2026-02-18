package com.dove.krxmarketdata.service;

import com.dove.stockdata.entity.MarketCalendar;
import com.dove.stockdata.enums.MarketDayType;
import com.dove.stockdata.enums.MarketType;
import com.dove.stockdata.repository.MarketCalendarRepository;
import com.dove.stockdata.service.StockDataSaveService;
import com.dove.krxmarketdata.dto.KrxStockInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class KrxStockDailySaveService {
    private final KrxStockService krxStockService;
    private final StockDataSaveService stockDataSaveService;
    private final MarketCalendarRepository marketCalendarRepository;

    /**
     * KOSPI + KOSDAQ 통합 조회 후 저장한다.
     * 둘 다 빈 리스트이면 휴장일(HOLIDAY)로 기록한다.
     * 데이터가 있으면 TRADING으로 기록하고 종목 데이터를 저장한다.
     */
    @Transactional
    public void saveDailyMarketData(LocalDate targetDate) {
        List<KrxStockInfo> kospiList = krxStockService
                .getStockListBy(MarketType.KOSPI, targetDate);
        List<KrxStockInfo> kosdaqList = krxStockService
                .getStockListBy(MarketType.KOSDAQ, targetDate);

        if (kospiList.isEmpty() && kosdaqList.isEmpty()) {
            marketCalendarRepository.save(new MarketCalendar(targetDate, MarketDayType.HOLIDAY));
            return;
        }

        marketCalendarRepository.save(new MarketCalendar(targetDate, MarketDayType.TRADING));
        kospiList.forEach(this::saveStockInfo);
        kosdaqList.forEach(this::saveStockInfo);
    }

    private void saveStockInfo(KrxStockInfo info) {
        stockDataSaveService.update(
                info.tradingDate(),
                info.marketType(),
                info.stockCode(),
                info.stockName(),
                info.tradingVolume(),
                info.openingPrice(),
                info.closingPrice(),
                info.lowestPrice(),
                info.highestPrice()
        );
    }
}
