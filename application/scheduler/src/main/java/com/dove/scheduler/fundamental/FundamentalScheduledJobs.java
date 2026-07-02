package com.dove.scheduler.fundamental;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 재무(DART) 정기 잡 — 일요일 고유번호 동기화. (신규·정정 폴링·밸류에이션은 일일 파이프라인 스텝으로 실행)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FundamentalScheduledJobs {

    private final FundamentalCollectionService collectionService;

    /**
     * 일요일 새벽 — DART 고유번호 재동기화(신규 상장 반영). 주가 조회 없는 조용한 시간대.
     */
    @Scheduled(cron = "${fundamental.corp-sync.cron:0 0 6 * * SUN}", zone = "Asia/Seoul")
    public void weeklyCorpSync() {
        int matched = collectionService.syncCorpCodes();
        log.info("[corp-sync] 매핑 {}건", matched);
    }
}
