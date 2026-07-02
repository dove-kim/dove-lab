package com.dove.scheduler.fundamental;

import com.dove.stock.application.service.StockShareCountService;
import com.dove.stock.domain.entity.StockShareCount;
import com.dove.stockcollection.application.port.ShareCountFetcher;
import com.dove.stockcollection.application.port.ShareCountRow;
import com.dove.stockcollection.application.service.CollectionProgress;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

/**
 * 상장주식수 수집 — KRX 일별시세에서 상장주식수를 받아 직전값과 다를 때만 변경이력으로 저장.
 * 시총 계산의 PIT 입력을 채운다. 재실행·겹친 조회에 멱등.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ShareCountCollectionService {

    private static final String SOURCE = "KRX";

    private final ShareCountFetcher fetcher;
    private final StockShareCountService shareCountService;

    /**
     * [from..to] 각 거래일의 상장주식수를 조회해 직전값과 다를 때만 저장한다(변경이력).
     * 최근 구간을 넓게 훑어 서버 장애·배포 누락을 다음 실행에서 회수한다.
     *
     * @return 변경 저장 건수
     */
    public int collect(LocalDate from, LocalDate to, CollectionProgress progress) {
        int totalDays = (int) (to.toEpochDay() - from.toEpochDay() + 1);
        progress.onTotal(totalDays);
        Map<String, Long> lastSeen = new HashMap<>();
        int saved = 0;
        int done = 0;
        for (LocalDate date = from; !date.isAfter(to); date = date.plusDays(1)) {
            for (ShareCountRow row : fetcher.fetch(date)) {
                Long prev = lastSeen.containsKey(row.ticker())
                        ? lastSeen.get(row.ticker())
                        : latestBefore(row.ticker(), date);
                if (prev == null || prev != row.listedShares()) {
                    shareCountService.save(new StockShareCount(row.ticker(), date, row.listedShares(), SOURCE));
                    saved++;
                }
                lastSeen.put(row.ticker(), row.listedShares());
            }
            progress.onProgress(++done);
        }
        log.info("[상장주식수 수집] {}~{} — 변경 저장 {}건", from, to, saved);
        return saved;
    }

    private Long latestBefore(String ticker, LocalDate date) {
        return shareCountService.findAsOf(ticker, date.minusDays(1))
                .map(StockShareCount::getListedShares)
                .orElse(null);
    }
}
