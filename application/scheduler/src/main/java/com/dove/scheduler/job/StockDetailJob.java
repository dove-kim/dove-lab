package com.dove.scheduler.job;

import com.dove.scheduler.service.InvestorCollectService;
import com.dove.scheduler.service.StockDetailService;
import com.dove.stockcollection.application.service.CollectionProgress;
import com.dove.stockcollection.application.service.StockEventCollectionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDate;

/**
 * 전 종목 KIS 상세 정보 upsert + 투자자매매동향 수집.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StockDetailJob {

    private final StockDetailService stockDetailService;
    private final InvestorCollectService investorCollectService;
    private final StockEventCollectionService eventCollectionService;
    private final Clock clock;

    /**
     * 종목 상세 정보·투자자매매동향·당일 권리 이벤트를 순차 수집한다.
     */
    @Scheduled(cron = "${stock.detail.cron:0 0 12 * * *}", zone = "Asia/Seoul")
    public void run() {
        LocalDate today = LocalDate.now(clock);
        log.info("StockDetailJob 시작: {}", today);

        // 종목 상세 정보 조회
        stockDetailService.updateAll();

        // 투자자 동향 조회
        investorCollectService.collectAll(today);

        // 당일 권리 이벤트(KSD) 수집 — 보조 정보라 실패해도 본 잡을 막지 않음
        try {
            eventCollectionService.collect(today, today, CollectionProgress.NOOP);
        } catch (Exception e) {
            log.error("당일 권리 이벤트 수집 실패: {}", e.getMessage(), e);
        }

        log.info("StockDetailJob 완료");
    }
}
