package com.dove.stockcollection.application.service;

import com.dove.concurrent.Parallel;
import com.dove.stockcollection.application.port.StockListing;
import com.dove.stockcollection.application.port.TradingDayPort;
import com.dove.market.domain.enums.MarketType;
import com.dove.stock.application.service.StockCommandService;
import com.dove.stock.application.service.StockTagValueService;
import com.dove.stock.domain.entity.Stock;
import com.dove.stock.domain.enums.TagField;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * KRX 종목 목록을 기간 단위로 수집한다. 신규 종목만 insert (멱등).
 */
@Slf4j
@Service
@ConditionalOnBean(TradingDayPort.class)
@RequiredArgsConstructor
public class StockCollectionService {

    private final TradingDayPort tradingDayPort;
    private final StockCommandService stockCommandService;
    private final StockTagValueService tagValueService;
    private final Clock clock;

    @Value("${collection.concurrency:40}")
    private int concurrency;

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

        // 날짜×시장 평탄화 — 각 조합이 독립 슬롯으로 경쟁
        List<Map.Entry<LocalDate, MarketType>> tasks = new ArrayList<>();
        for (LocalDate d = to; !d.isBefore(from); d = d.minusDays(1)) {
            for (MarketType market : MarketType.KRX_MARKETS) {
                tasks.add(Map.entry(d, market));
            }
        }

        progress.onTotal(tasks.size());
        log.info("종목 수집 시작: {}~{} ({}건)", from, to, tasks.size());

        AtomicInteger processed = new AtomicInteger();
        // ticker 중복 제거용 맵 — 여러 날짜·시장에 나와도 한 번만 insert
        Map<String, Stock> stockMap = new ConcurrentHashMap<>();
        Set<String> secugrps = ConcurrentHashMap.newKeySet();
        Set<String> kinds = ConcurrentHashMap.newKeySet();

        Parallel.run(tasks, concurrency, entry -> {
            LocalDate date = entry.getKey();
            MarketType market = entry.getValue();
            List<StockListing> listings = tradingDayPort.fetchListings(market, date);
            listings.forEach(l -> {
                stockMap.putIfAbsent(l.ticker(),
                        new Stock(l.ticker(), l.isin(), market, l.listingDate(),
                                l.secugrpNm(), l.kindStkCertTpNm()));
                if (l.secugrpNm() != null) secugrps.add(l.secugrpNm());
                if (l.kindStkCertTpNm() != null) kinds.add(l.kindStkCertTpNm());
            });
            progress.onProgress(processed.incrementAndGet());
        });

        // 모두 끝난 뒤 한 번만 insert
        if (!stockMap.isEmpty()) {
            stockCommandService.insertIfAbsent(new ArrayList<>(stockMap.values()));
        }

        // 분류값 마스터 등록
        secugrps.forEach(v -> tagValueService.registerIfAbsent(TagField.SECUGRP.name(), v));
        kinds.forEach(v -> tagValueService.registerIfAbsent(TagField.STOCK_TYPE.name(), v));

        log.info("종목 수집 완료: {}~{}", from, to);
    }
}
