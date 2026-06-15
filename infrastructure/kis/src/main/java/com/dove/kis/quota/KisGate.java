package com.dove.kis.quota;

import com.dove.apiquota.RateLimiter;
import feign.FeignException;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.util.concurrent.Semaphore;
import java.util.function.Supplier;

/**
 * KIS 주식 API 호출 게이트. rate + concurrency 이중 제한.
 */
@Slf4j
public class KisGate {

    private static final long RETRY_WAIT_MS = 1_000;

    private final RateLimiter rateLimiter;
    private final Semaphore semaphore;
    private final int maxRetries;
    private final Duration acquireTimeout;

    public KisGate(RateLimiter rateLimiter, int maxConcurrent, int maxRetries, Duration acquireTimeout) {
        this.rateLimiter = rateLimiter;
        this.semaphore = new Semaphore(maxConcurrent);
        this.maxRetries = maxRetries;
        this.acquireTimeout = acquireTimeout;
    }

    /**
     * KIS API 호출을 rate + concurrency 이중 제한 안에서 실행한다.
     * rate 슬롯을 제한 시간 안에 못 받거나 KIS 일시오류면 최대 {@code maxRetries}회 재시도한다.
     *
     * @throws KisRateLimitException 슬롯 대기 인터럽트 또는 rate 슬롯 획득이 재시도 한도까지 실패한 경우
     */
    public <R> R call(Supplier<R> kisCall) {
        int attempt = 0;
        while (true) {
            // 순번 대기(상한). 시간 내 못 받으면 일시 문제로 간주 — 무한 대기 방지.
            if (!acquireRateSlot()) {
                if (attempt < maxRetries) {
                    attempt++;
                    if (attempt == maxRetries) log.warn("KIS rate 슬롯 타임아웃 — 마지막 재시도 ({}번째)", attempt);
                    sleep();
                    continue;
                }
                log.error("KIS rate 슬롯 타임아웃 — {}회 재시도 모두 실패, 배치 중단", maxRetries);
                throw new KisRateLimitException("rate 슬롯 획득 타임아웃");
            }

            try { semaphore.acquire(); }
            catch (InterruptedException e) { Thread.currentThread().interrupt(); throw new KisRateLimitException("semaphore 슬롯 대기 중 인터럽트"); }

            boolean shouldRetry = false;
            try {
                return kisCall.get();
            } catch (FeignException e) {
                boolean transientError = KisErrorCodes.isTransient(e);
                if (transientError && attempt < maxRetries) {
                    attempt++;
                    shouldRetry = true;
                    if (attempt == maxRetries) log.warn("KIS 일시오류 — 마지막 재시도 ({}번째)", attempt);
                } else {
                    if (transientError) log.error("KIS 일시오류 — {}회 재시도 모두 실패, 배치 중단", maxRetries);
                    throw e;
                }
            } finally {
                semaphore.release(); // 성공·실패 모두 즉시 반납
            }

            // 슬롯 없이 대기 후 재진입
            if (shouldRetry) sleep();
        }
    }

    /**
     * rate 슬롯을 제한 시간 안에 획득한다.
     */
    private boolean acquireRateSlot() {
        try {
            return rateLimiter.tryAcquire(acquireTimeout);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new KisRateLimitException("rate 슬롯 대기 중 인터럽트");
        }
    }

    private void sleep() {
        try {
            Thread.sleep(RETRY_WAIT_MS);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            throw new KisRateLimitException("재시도 대기 중 인터럽트");
        }
    }
}
