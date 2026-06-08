package com.dove.stockcollection.application.service;

import com.dove.stockcollection.application.port.StockListing;
import com.dove.stockcollection.application.port.TradingDayPort;
import com.dove.market.domain.enums.MarketType;
import com.dove.stock.application.service.StockCommandService;
import com.dove.stock.domain.entity.Stock;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

/**
 * KRX 종목 목록을 기간 단위로 수집한다. 신규 종목만 insert (멱등).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StockCollectionService {

    private final TradingDayPort tradingDayPort;
    private final StockCommandService stockCommandService;
    private final Clock clock;

    /**
     * 지정 기간의 KRX 종목을 수집한다. {@code to}가 오늘 이후면 오늘로 캡한다.
     */
    public void collect(LocalDate from, LocalDate to, CollectionProgress progress) {
        LocalDate today = LocalDate.now(clock);
        if (from.isAfter(today)) {
            log.warn("{} 이후는 미래 — 수집 안 함", from);
            progress.onTotal(0);
            return;
        }
        if (to.isAfter(today)) to = today;

        int totalDays = (int) (to.toEpochDay() - from.toEpochDay() + 1);
        progress.onTotal(totalDays);
        log.info("종목 수집 시작: {}~{}", from, to);

        int processed = 0;
        for (LocalDate date = to; !date.isBefore(from); date = date.minusDays(1)) {
            for (MarketType market : MarketType.KRX_MARKETS) {
                List<StockListing> listings = tradingDayPort.fetchListings(market, date);
                if (listings.isEmpty()) continue;
                List<Stock> stocks = listings.stream()
                        .map(l -> new Stock(l.ticker(), l.isin(), market, l.listingDate(),
                                l.secugrpNm(), l.kindStkCertTpNm()))
                        .toList();
                stockCommandService.insertIfAbsent(stocks);
            }
            progress.onProgress(++processed);
        }

        log.info("종목 수집 완료: {}~{}", from, to);
    }
}
