package com.dove.kis.quota;

import com.dove.apiquota.RateLimiter;
import feign.FeignException;
import lombok.extern.slf4j.Slf4j;

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

    public KisGate(RateLimiter rateLimiter, int maxConcurrent, int maxRetries) {
        this.rateLimiter = rateLimiter;
        this.semaphore = new Semaphore(maxConcurrent);
        this.maxRetries = maxRetries;
    }

    /**
     * KIS API 호출을 rate + concurrency 이중 제한 안에서 실행한다.
     *
     * @throws KisRateLimitException 슬롯 대기 중 인터럽트 발생 시
     */
    public <R> R call(Supplier<R> kisCall) {
        int attempt = 0;
        while (true) {
            // 매 시도마다 rate + semaphore 획득
            try { rateLimiter.acquire(); }
            catch (InterruptedException e) { Thread.currentThread().interrupt(); throw new KisRateLimitException("rate 슬롯 대기 중 인터럽트"); }
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
            if (shouldRetry) {
                try { Thread.sleep(RETRY_WAIT_MS); }
                catch (InterruptedException ie) { Thread.currentThread().interrupt(); throw new KisRateLimitException("재시도 대기 중 인터럽트"); }
            }
        }
    }
}
