package com.dove.scheduler.job;

import com.dove.scheduler.service.PortfolioMarketDataService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 포트폴리오 시세·환율 일배치 진입점 — 미국장 마감 이후 아침에 실행한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PortfolioMarketDataJob {

    private final PortfolioMarketDataService service;

    @Scheduled(cron = "${portfolio.marketdata.cron:0 30 7 * * *}", zone = "Asia/Seoul")
    public void run() {
        service.refresh();
    }
}
