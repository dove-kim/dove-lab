package com.dove.scheduler.service;

import com.dove.krx.StockListing;
import com.dove.krx.TradingDayPort;
import com.dove.market.domain.enums.MarketType;
import com.dove.stock.application.service.StockCommandService;
import com.dove.stock.application.service.StockTagValueService;
import com.dove.stock.domain.entity.Stock;
import com.dove.stock.domain.enums.TagField;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * KRX 종목 동기화. StockSyncJob(08:05)에서 최근 N일치를 수집한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StockSyncService {

    private final TradingDayPort tradingDayPort;
    private final StockCommandService stockCommandService;
    private final StockTagValueService tagValueCommandService;

    /**
     * from~to 범위의 KRX 종목을 날짜별로 조회해 ticker 기준으로 중복 제거 후 신규 종목만 insert한다.
     * 동일 ticker가 여러 날짜에 나오면 가장 최신 날짜 데이터를 사용한다.
     */
    public void syncRange(MarketType market, LocalDate from, LocalDate to, Runnable onDateFetched) {
        Map<String, StockListing> unique = new LinkedHashMap<>();
        for (LocalDate date = from; !date.isAfter(to); date = date.plusDays(1)) {
            for (StockListing l : tradingDayPort.fetchListings(market, date)) {
                unique.put(l.ticker(), l); // 같은 ticker는 최신 날짜로 덮어씀
            }
            onDateFetched.run();
        }
        if (unique.isEmpty()) {
            log.warn("[{}] {}~{} 기간 수집된 종목 없음", market, from, to);
            return;
        }
        List<Stock> stocks = unique.values().stream()
                .map(l -> new Stock(l.ticker(), l.isin(), market, l.listingDate(), l.secugrpNm(), l.kindStkCertTpNm()))
                .toList();
        stockCommandService.insertIfAbsent(stocks);
        registerTagValues(unique.values());
        log.info("[{}] 종목 동기화 완료 ({}~{}): {}종목 수집", market, from, to, stocks.size());
    }

    /** 증권그룹·주권종류 분류 값을 distinct로 마스터에 등록한다. */
    private void registerTagValues(Iterable<StockListing> listings) {
        Set<String> secugrps = new HashSet<>();
        Set<String> kinds = new HashSet<>();
        for (StockListing l : listings) {
            if (l.secugrpNm() != null) secugrps.add(l.secugrpNm());
            if (l.kindStkCertTpNm() != null) kinds.add(l.kindStkCertTpNm());
        }
        secugrps.forEach(v -> tagValueCommandService.registerIfAbsent(TagField.SECUGRP.name(), v));
        kinds.forEach(v -> tagValueCommandService.registerIfAbsent(TagField.STOCK_TYPE.name(), v));
    }
}
