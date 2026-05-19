package com.dove.scheduler;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.time.Clock;
import java.time.ZoneId;
import java.util.concurrent.ForkJoinPool;

@EnableScheduling
@SpringBootApplication
@ComponentScan(basePackages = {"com.dove"})
public class SchedulerApplication {

    public static void main(String[] args) {
        SpringApplication.run(SchedulerApplication.class, args);
    }

    @Bean
    public Clock clock() {
        return Clock.system(ZoneId.of("Asia/Seoul"));
    }

    /** 시장 데이터 수집·지표 계산 전용 스레드 풀. */
    @Bean
    public ThreadPoolTaskExecutor taskExecutor(
            @Value("${scheduler.thread-pool.core-size:4}") int coreSize,
            @Value("${scheduler.thread-pool.max-size:8}") int maxSize,
            @Value("${scheduler.thread-pool.queue-capacity:200}") int queueCapacity) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(coreSize);
        executor.setMaxPoolSize(maxSize);
        executor.setQueueCapacity(queueCapacity);
        executor.setThreadNamePrefix("batch-worker-");
        executor.setRejectedExecutionHandler(new java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        return executor;
    }

    /** 시장별 병렬 처리 전용 ForkJoinPool — commonPool 점유 방지. */
    @Bean
    public ForkJoinPool marketParallelPool(
            @Value("${scheduler.market-parallelism:2}") int parallelism) {
        return new ForkJoinPool(parallelism);
    }
}
