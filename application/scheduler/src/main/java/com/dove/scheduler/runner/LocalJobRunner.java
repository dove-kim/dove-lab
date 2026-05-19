package com.dove.scheduler.runner;

import com.dove.scheduler.job.CompletionScanJob;
import com.dove.scheduler.job.DailyMarketJob;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * 로컬 개발용 Job 실행기.
 *
 * <p>'local' 프로파일에서만 활성화된다.
 * {@code JOB} 환경변수로 잡을 지정하면 즉시 실행 후 프로세스가 종료된다.
 * 잡 실행 후 바로 종료되므로 스케줄 cron은 발동하지 않는다.
 *
 * <pre>
 * SPRING_PROFILES_ACTIVE=local JOB=daily \
 *   MARKET_INITIAL_DATE=2026-01-01 KRX_API_AUTH_KEY=<key> \
 *   ./gradlew :scheduler:bootRun
 * </pre>
 */
@Slf4j
@Component
@Profile("local")
public class LocalJobRunner implements ApplicationRunner {

    private final DailyMarketJob dailyMarketJob;
    private final CompletionScanJob completionScanJob;
    private final ApplicationContext context;
    private final String job;

    public LocalJobRunner(
            DailyMarketJob dailyMarketJob,
            CompletionScanJob completionScanJob,
            ApplicationContext context,
            @Value("${JOB:}") String job) {
        this.dailyMarketJob = dailyMarketJob;
        this.completionScanJob = completionScanJob;
        this.context = context;
        this.job = job;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (job.isBlank()) {
            log.warn("[로컬] JOB 환경변수가 없습니다. JOB=daily 또는 JOB=completion 을 지정하세요.");
            exit(1);
            return;
        }

        switch (job) {
            case "daily" -> {
                log.info("[로컬] DailyMarketJob 실행");
                dailyMarketJob.run();
            }
            case "completion" -> {
                log.info("[로컬] CompletionScanJob 실행");
                completionScanJob.run();
            }
            default -> {
                log.error("[로컬] 알 수 없는 JOB 값: '{}'. daily 또는 completion 을 지정하세요.", job);
                exit(1);
                return;
            }
        }

        exit(0);
    }

    private void exit(int code) {
        System.exit(SpringApplication.exit(context, () -> code));
    }
}
