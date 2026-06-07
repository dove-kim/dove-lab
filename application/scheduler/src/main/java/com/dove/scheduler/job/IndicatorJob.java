package com.dove.scheduler.job;

import com.dove.scheduler.service.IndicatorComputeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDate;

/**
 * 커서 기반 기술적 지표 계산.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class IndicatorJob {

    private final IndicatorComputeService indicatorComputeService;
    private final Clock clock;

    /**
     * 전 그룹의 기술적 지표를 커서 기준으로 계산한다.
     */
    @Scheduled(cron = "${indicator.cron:0 0 0 * * *}", zone = "Asia/Seoul")
    public void run() {
        LocalDate today = LocalDate.now(clock);
        log.info("IndicatorJob 시작: {}", today);
        indicatorComputeService.computeAll(today);
        log.info("IndicatorJob 완료");
    }
}
