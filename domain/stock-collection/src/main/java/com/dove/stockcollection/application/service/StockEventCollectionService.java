package com.dove.stockcollection.application.service;

import com.dove.concurrent.Parallel;
import com.dove.stock.application.service.StockEventService;
import com.dove.stock.application.service.StockQueryService;
import com.dove.stock.domain.enums.StockEventType;
import com.dove.stockcollection.application.port.KsdEventFetcher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * KIS 예탁원정보(KSD) 권리 이벤트 수집 코어. 일일 수집(당일)·재조회(기간)가 공통 사용한다.
 *
 * <p>KSD는 호출당 100행 상한이고 연속조회가 안 되므로 완전성 전략을 나눈다:
 * <ul>
 *   <li><b>일일(단일일)</b>: 날짜범위 1콜/유형. 100행에 걸리면(월말 배당 등) 그날만 종목별 조회로 보완.</li>
 *   <li><b>백필(기간)</b>: 종목별 전 구간 조회(종목당 100 미만이라 캡 없음, 완전).</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StockEventCollectionService {

    private static final int PAGE_LIMIT = 100;   // KSD 호출당 최대 행 (이 수면 캡 의심)

    @Value("${collection.concurrency:40}")
    private int concurrency;

    private final KsdEventFetcher fetcher;
    private final StockEventService eventCommandService;
    private final StockQueryService stockQueryService;
    private final KsdEventRowMapper rowMapper;

    /**
     * from~to 기간의 전 유형 권리 이벤트를 수집한다.
     */
    public void collect(LocalDate from, LocalDate to, CollectionProgress progress) {
        StockEventType[] types = StockEventType.values();

        if (!from.equals(to)) {
            // 백필 = 종목별 전 구간. 진행률을 (유형 × 종목) 단위로 세밀하게 → ETA 정확.
            List<String> tickers = stockQueryService.findAllTickers();
            progress.onTotal(types.length * tickers.size());
            AtomicInteger done = new AtomicInteger();
            for (StockEventType type : types) {
                List<Map<String, Object>> rows = fetchPerStock(type, from, to, tickers, progress, done);
                log.info("[EVENT] {} {}~{}: {}건 수신, {}건 처리", type, from, to, rows.size(), save(type, rows));
            }
            return;
        }

        // 일일(단일일) = 날짜범위 1콜/유형(빠름). 100건이면 그날만 종목별 보완. 진행률은 유형 단위로 충분.
        progress.onTotal(types.length);
        List<String> tickers = null;
        int done = 0;
        for (StockEventType type : types) {
            List<Map<String, Object>> rows = fetcher.fetch(type, from, to, "");
            if (rows.size() >= PAGE_LIMIT) {
                if (tickers == null) tickers = stockQueryService.findAllTickers();
                rows = fetchPerStock(type, from, to, tickers, null, null);
            }
            log.info("[EVENT] {} {}~{}: {}건 수신, {}건 처리", type, from, to, rows.size(), save(type, rows));
            progress.onProgress(++done);
        }
    }

    /**
     * 종목별로 병렬 조회해 합친다 (종목당 이벤트는 100 미만이라 캡 없음 = 완전).
     */
    private List<Map<String, Object>> fetchPerStock(StockEventType type, LocalDate from, LocalDate to,
                                                    List<String> tickers,
                                                    CollectionProgress progress, AtomicInteger done) {
        List<Map<String, Object>> all = Collections.synchronizedList(new ArrayList<>());
        Parallel.run(tickers, concurrency, ticker -> {
            List<Map<String, Object>> rows = fetcher.fetch(type, from, to, ticker);
            if (!rows.isEmpty()) all.addAll(rows);
            if (progress != null) progress.onProgress(done.incrementAndGet());
        });
        return all;
    }

    /**
     * 행 목록을 STOCK_EVENT에 upsert. 실제 신규 저장 건수 반환.
     */
    private int save(StockEventType type, List<Map<String, Object>> rows) {
        int saved = 0;
        for (Map<String, Object> row : rows) {
            String ticker = rowMapper.ticker(row);
            LocalDate date = rowMapper.recordDate(row);
            if (ticker.isBlank() || date == null) continue;
            if (eventCommandService.saveIfAbsent(ticker, type, date, rowMapper.summary(type, row), rowMapper.toJson(row))) {
                saved++;
            }
        }
        return saved;
    }
}
