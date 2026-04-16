package com.dove.krxmarketdata.application.service;

import com.dove.stockdata.domain.entity.MarketCalendar;
import com.dove.stockdata.domain.entity.MarketCalendarId;
import com.dove.stockdata.domain.enums.MarketDayType;
import com.dove.stockdata.domain.enums.MarketType;
import com.dove.stockdata.domain.repository.MarketCalendarRepository;
import com.dove.stockdata.application.service.StockDataSaveService;
import com.dove.krxmarketdata.application.dto.KrxStockInfo;
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
     * 단일 시장의 주식 데이터를 조회 후 저장한다.
     * 데이터가 없으면 해당 시장을 HOLIDAY로, 있으면 TRADING으로 기록한다.
     *
     * @return 저장된 종목 코드 목록. 휴장일이면 빈 리스트.
     */
    @Transactional
    public List<String> saveDailyMarketDataByMarket(LocalDate targetDate, MarketType marketType) {
        List<KrxStockInfo> stockList = krxStockService.getStockListBy(marketType, targetDate);

        if (stockList.isEmpty()) {
            marketCalendarRepository.save(new MarketCalendar(targetDate, marketType, MarketDayType.HOLIDAY));
            return List.of();
        }

        saveOrUpdateCalendar(targetDate, marketType, MarketDayType.TRADING);
        stockList.forEach(this::saveStockInfo);
        return stockList.stream().map(KrxStockInfo::stockCode).toList();
    }

    private void saveOrUpdateCalendar(LocalDate date, MarketType marketType, MarketDayType dayType) {
        marketCalendarRepository.findById(new MarketCalendarId(date, marketType))
                .ifPresentOrElse(
                        existing -> existing.updateDayType(dayType),
                        () -> marketCalendarRepository.save(new MarketCalendar(date, marketType, dayType))
                );
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

